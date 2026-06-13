package io.github.delanym.maven.notifier.channel.discord;

import io.github.delanym.maven.notifier.core.HttpNotifier;
import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.NotificationLevel;
import org.jspecify.annotations.NullMarked;

import java.net.URI;
import java.net.http.HttpClient;

/**
 * Sends build notifications to a Discord channel via an incoming webhook.
 * Uses Discord's embed format with color-coded sidebars.
 *
 * <p>Configuration properties:</p>
 * <ul>
 *   <li>{@code notifier.discord.webhook} — the webhook URL (required)</li>
 * </ul>
 */
@NullMarked
public final class DiscordNotifier extends HttpNotifier {

    private final URI webhookUri;

    public DiscordNotifier(String webhookUrl) {
        this.webhookUri = URI.create(webhookUrl);
    }

    public DiscordNotifier(String webhookUrl, HttpClient httpClient) {
        super(httpClient);
        this.webhookUri = URI.create(webhookUrl);
    }

    @Override
    public String name() {
        return "discord";
    }

    @Override
    public void send(Notification notification) {
        String json = buildPayload(notification);
        postJson(webhookUri, json);
    }

    private String buildPayload(Notification notification) {
        int color = colorFor(notification.level());
        return """
                {
                  "embeds": [
                    {
                      "title": "%s",
                      "description": "%s",
                      "color": %d
                    }
                  ]
                }""".formatted(
                escapeJson(notification.title()),
                escapeJson(notification.message()),
                color
        );
    }

    /**
     * Discord embed colors are decimal representations of hex color codes.
     */
    private int colorFor(NotificationLevel level) {
        return switch (level) {
            case INFO -> 3066993;
            case WARNING -> 15105570;
            case ERROR -> 15158332;
        };
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
