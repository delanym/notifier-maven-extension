package io.github.delanym.maven.notifier.maven;

import io.github.delanym.maven.notifier.core.CommandExecutor;
import io.github.delanym.maven.notifier.core.CompositeNotifier;
import io.github.delanym.maven.notifier.core.NotifierConfiguration;
import io.github.delanym.maven.notifier.core.DoNothingNotifier;
import io.github.delanym.maven.notifier.core.Icon;
import io.github.delanym.maven.notifier.core.Notifier;
import io.github.delanym.maven.notifier.core.NotifierException;
import io.github.delanym.maven.notifier.channel.burnttoast.BurntToastNotifier;
import io.github.delanym.maven.notifier.channel.discord.DiscordNotifier;
import io.github.delanym.maven.notifier.channel.mastodon.MastodonNotifier;
import io.github.delanym.maven.notifier.channel.mqtt.MqttNotifier;
import io.github.delanym.maven.notifier.channel.notifysend.NotifySendNotifier;
import io.github.delanym.maven.notifier.channel.slack.SlackNotifier;
import io.github.delanym.maven.notifier.channel.systemtray.SystemTrayNotifier;
import io.github.delanym.maven.notifier.channel.teams.TeamsNotifier;
import io.github.delanym.maven.notifier.channel.telegram.TelegramNotifier;
import io.github.delanym.maven.notifier.channel.terminalnotifier.TerminalNotifierNotifier;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Creates and wires up {@link Notifier} instances based on user configuration.
 * Supports explicit selection by name (including comma-separated lists for
 * multi-channel notify) and OS-aware auto-discovery.
 */
@NullMarked
public final class NotifierRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierRegistry.class);
    private static final String TRAY_ICON = "emoji_u2705.png";

    public Notifier resolve(NotifierConfiguration config) {
        String implementation = config.implementation();
        if ("auto".equals(implementation)) {
            return autoDiscover(config);
        }
        if (implementation.contains(",")) {
            return resolveMultiple(implementation, config);
        }
        return resolveByName(implementation.trim(), config);
    }

    private Notifier resolveMultiple(String implementations, NotifierConfiguration config) {
        List<Notifier> notifiers = new ArrayList<>();
        for (String name : implementations.split(",")) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                notifiers.add(resolveByName(trimmed, config));
            }
        }
        return switch (notifiers.size()) {
            case 0 -> DoNothingNotifier.instance();
            case 1 -> notifiers.getFirst();
            default -> new CompositeNotifier(notifiers);
        };
    }

    private Notifier resolveByName(String name, NotifierConfiguration config) {
        Properties props = config.notifierProperties();
        return switch (name) {
            case "burnttoast" -> new BurntToastNotifier(
                    props.getProperty("notifier.burnttoast.sound")
            );
            case "terminal-notifier" -> new TerminalNotifierNotifier(
                    new CommandExecutor(),
                    props.getProperty("notifier.terminal-notifier.bin", "terminal-notifier"),
                    props.getProperty("notifier.terminal-notifier.activateApplication"),
                    props.getProperty("notifier.terminal-notifier.sound")
            );
            case "notify-send" -> new NotifySendNotifier(
                    new CommandExecutor(),
                    props.getProperty("notifier.notify-send.bin", "notify-send"),
                    config.timeoutMs()
            );
            case "systemtray" -> new SystemTrayNotifier(
                    "Maven",
                    Icon.of(getClass().getClassLoader().getResource(TRAY_ICON), "systemtray"),
                    config.timeoutMs()
            );
            case "slack" -> new SlackNotifier(
                    requireProperty(props, "notifier.slack.webhook")
            );
            case "telegram" -> new TelegramNotifier(
                    requireProperty(props, "notifier.telegram.token"),
                    requireProperty(props, "notifier.telegram.chatId")
            );
            case "teams" -> new TeamsNotifier(
                    requireProperty(props, "notifier.teams.webhook")
            );
            case "discord" -> new DiscordNotifier(
                    requireProperty(props, "notifier.discord.webhook")
            );
            case "mastodon" -> new MastodonNotifier(
                    requireProperty(props, "notifier.mastodon.instanceUrl"),
                    requireProperty(props, "notifier.mastodon.accessToken"),
                    props.getProperty("notifier.mastodon.visibility", "unlisted")
            );
            case "mqtt" -> new MqttNotifier(
                    requireProperty(props, "notifier.mqtt.brokerUrl"),
                    props.getProperty("notifier.mqtt.topic", "notifier/builds"),
                    props.getProperty("notifier.mqtt.clientId", "notifier-maven"),
                    props.getProperty("notifier.mqtt.username"),
                    props.getProperty("notifier.mqtt.password")
            );
            default -> {
                LOGGER.warn("Unknown notifier '{}'; using no-op notifier", name);
                yield DoNothingNotifier.instance();
            }
        };
    }

    private Notifier autoDiscover(NotifierConfiguration config) {
        String os = System.getProperty("os.name", "").toLowerCase();
        List<Notifier> candidates;
        if (os.contains("win")) {
            candidates = List.of(
                    resolveByName("burnttoast", config),
                    resolveByName("systemtray", config)
            );
        } else if (os.contains("mac")) {
            candidates = List.of(
                    resolveByName("terminal-notifier", config),
                    resolveByName("systemtray", config)
            );
        } else {
            candidates = List.of(
                    resolveByName("notify-send", config),
                    resolveByName("systemtray", config)
            );
        }

        for (Notifier candidate : candidates) {
            if (candidate.isAvailable()) {
                LOGGER.debug("Auto-discovered notifier: {}", candidate.name());
                return candidate;
            }
        }
        LOGGER.info("No active notifier found: notifications will not be sent.");
        return DoNothingNotifier.instance();
    }

    private String requireProperty(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new NotifierException(
                    "Required configuration property missing: " + key
            );
        }
        return value;
    }
}
