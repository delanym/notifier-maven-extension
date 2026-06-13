package io.github.delanym.maven.notifier.core;

import org.jspecify.annotations.NullMarked;

/**
 * Contract for a notification channel. Implementations deliver build
 * notifications to a specific target (desktop toast, chat service, feed, etc.).
 *
 * <p>Lifecycle: {@link #init()} is called once before any {@link #send} calls,
 * and {@link #close()} is called once during Maven shutdown.</p>
 */
@NullMarked
public interface Notifier extends AutoCloseable {

    /**
     * Unique name used for configuration lookup and logging.
     */
    String name();

    /**
     * Perform any setup required before sending (e.g. connection pooling,
     * verifying external tool availability).
     */
    void init();

    /**
     * Send a notification to this channel.
     */
    void send(Notification notification);

    /**
     * Release resources. Called during Maven JVM shutdown.
     */
    @Override
    void close();

    /**
     * Whether this notifier should always fire regardless of build-duration
     * threshold. Persistent notifiers (e.g. system tray) typically return
     * {@code true}.
     */
    default boolean isPersistent() {
        return false;
    }

    /**
     * Probe whether this notifier is available on the current system.
     * Used during auto-discovery when no explicit implementation is configured.
     */
    default boolean isAvailable() {
        return false;
    }
}
