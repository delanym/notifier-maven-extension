package io.github.delanym.maven.notifier.core;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A no-op notifier used as a fallback when no suitable notification channel
 * is found or configured. Logs a debug message on each send.
 */
@NullMarked
public final class DoNothingNotifier implements Notifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoNothingNotifier.class);
    private static final DoNothingNotifier INSTANCE = new DoNothingNotifier();

    private DoNothingNotifier() {
    }

    public static DoNothingNotifier instance() {
        return INSTANCE;
    }

    @Override
    public String name() {
        return "none";
    }

    @Override
    public void close() {
    }

    @Override
    public void init() {
    }

    @Override
    public void send(Notification notification) {
        LOGGER.debug("No notifier configured; dropping notification: {}", notification.title());
    }
}
