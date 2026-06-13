package io.github.delanym.maven.notifier.channel.notifysend;

import io.github.delanym.maven.notifier.core.CommandExecutor;
import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.NotificationLevel;
import io.github.delanym.maven.notifier.core.Notifier;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

/**
 * Linux desktop notifications via the {@code notify-send} command-line tool,
 * part of libnotify. Maps notification levels to urgency values (low, normal,
 * critical) understood by the freedesktop notification daemon.
 */
@NullMarked
public final class NotifySendNotifier implements Notifier {

    private final CommandExecutor executor;
    private final String binary;
    private final long timeoutMs;

    public NotifySendNotifier(CommandExecutor executor, String binary, long timeoutMs) {
        this.executor = executor;
        this.binary = binary;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public void close() {
    }

    @Override
    public void init() {
    }

    @Override
    public boolean isAvailable() {
        return executor.isCommandAvailable(binary, "--help");
    }

    @Override
    public String name() {
        return "notify-send";
    }

    @Override
    public void send(Notification notification) {
        List<String> command = new ArrayList<>();
        command.add(binary);
        command.add(notification.title());
        command.add(notification.message());

        if (timeoutMs > 0) {
            command.add("-t");
            command.add(String.valueOf(timeoutMs));
        }

        command.add("-h");
        command.add("string:image-path:" + notification.icon().asPath());

        command.add("-u");
        command.add(toUrgency(notification.level()));

        executor.execute(command.toArray(String[]::new));
    }

    private String toUrgency(NotificationLevel level) {
        return switch (level) {
            case ERROR, WARNING -> "critical";
            case INFO -> "normal";
        };
    }
}
