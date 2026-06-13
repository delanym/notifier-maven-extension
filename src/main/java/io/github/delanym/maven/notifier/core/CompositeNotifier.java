package io.github.delanym.maven.notifier.core;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Sends notifications to multiple channels sequentially. If one channel
 * fails, the remaining channels still receive the notification.
 */
@NullMarked
public final class CompositeNotifier implements Notifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeNotifier.class);

    private final List<Notifier> delegates;

    public CompositeNotifier(List<Notifier> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public String name() {
        return "composite";
    }

    @Override
    public void close() {
      for (Notifier delegate : delegates) {
        try {
          delegate.close();
        } catch (Exception e) {
          LOGGER.warn("Error closing notifier {}: {} ({})", delegate.name(), e.getMessage(), e.getClass().getSimpleName());
        }
      }
    }

    @Override
    public void init() {
        for (Notifier delegate : delegates) {
            delegate.init();
        }
    }

    @Override
    public boolean isPersistent() {
        return delegates.stream().anyMatch(Notifier::isPersistent);
    }

    @Override
    public void send(Notification notification) {
        for (Notifier delegate : delegates) {
            try {
                delegate.send(notification);
            } catch (Exception e) {
                LOGGER.warn("Notifier {} failed to send: {} ({})", delegate.name(), e.getMessage(), e.getClass().getSimpleName());
            }
        }
    }
}
