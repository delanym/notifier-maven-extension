package io.github.delanym.maven.notifier.channel.systemtray;

import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.NotificationLevel;
import io.github.delanym.maven.notifier.core.Notifier;
import io.github.delanym.maven.notifier.core.Icon;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Cross-platform fallback notifier using the Java AWT {@link SystemTray} API.
 * Displays a tray icon with popup notifications. This notifier is persistent:
 * the icon remains in the system tray until explicitly closed.
 */
@NullMarked
public final class SystemTrayNotifier implements Notifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemTrayNotifier.class);
    private static final long DEFAULT_DISPLAY_MS = 2000;

    private final String applicationName;
    private final Icon applicationIcon;
    private final long displayMs;
    private @Nullable TrayIcon trayIcon;

    public SystemTrayNotifier(
            String applicationName,
            Icon applicationIcon,
            long displayMs) {
        this.applicationName = applicationName;
        this.applicationIcon = applicationIcon;
        this.displayMs = displayMs > 0 ? displayMs : DEFAULT_DISPLAY_MS;
    }

    @Override
    public void close() {
        TrayIcon icon = this.trayIcon;
        if (icon != null) {
            try {
                Thread.sleep(displayMs);
                SystemTray.getSystemTray().remove(icon);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void init() {
        if (!SystemTray.isSupported()) {
            LOGGER.warn("SystemTray is not supported on this platform");
            return;
        }
        Image image = Toolkit.getDefaultToolkit().getImage(applicationIcon.content());
        trayIcon = new TrayIcon(image, applicationName);
        trayIcon.setImageAutoSize(true);
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException e) {
            LOGGER.warn("Failed to add system tray icon", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return SystemTray.isSupported();
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public String name() {
        return "systemtray";
    }

    @Override
    public void send(Notification notification) {
        TrayIcon icon = this.trayIcon;
        if (icon == null) {
            LOGGER.warn("SystemTray not initialized; cannot send notification");
            return;
        }
        icon.displayMessage(
                notification.title(),
                notification.message(),
                toMessageType(notification.level())
        );
    }

    private TrayIcon.MessageType toMessageType(NotificationLevel level) {
        return switch (level) {
            case ERROR -> TrayIcon.MessageType.ERROR;
            case WARNING -> TrayIcon.MessageType.WARNING;
            case INFO -> TrayIcon.MessageType.INFO;
        };
    }
}
