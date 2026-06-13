package io.github.delanym.maven.notifier.core;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * An immutable notification payload containing the content to be notifyed
 * through one or more notification channels.
 */
@NullMarked
public final class Notification {

    private final String title;
    private final String message;
    private final @Nullable String subtitle;
    private final Icon icon;
    private final NotificationLevel level;

    private Notification(Builder builder) {
        this.title = Objects.requireNonNull(builder.title, "title");
        this.message = Objects.requireNonNull(builder.message, "message");
        this.subtitle = builder.subtitle;
        this.icon = Objects.requireNonNull(builder.icon, "icon");
        this.level = Objects.requireNonNull(builder.level, "level");
    }

    public static Builder builder() {
        return new Builder();
    }

    public String title() {
        return title;
    }

    public String message() {
        return message;
    }

    public @Nullable String subtitle() {
        return subtitle;
    }

    public Icon icon() {
        return icon;
    }

    public NotificationLevel level() {
        return level;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof Notification other
                && Objects.equals(title, other.title)
                && Objects.equals(message, other.message)
                && Objects.equals(subtitle, other.subtitle)
                && Objects.equals(icon, other.icon)
                && level == other.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, message, subtitle, icon, level);
    }

    @Override
    public String toString() {
        return "Notification{title='" + title + "', level=" + level + "}";
    }

    public static final class Builder {

        private @Nullable String title;
        private @Nullable String message;
        private @Nullable String subtitle;
        private @Nullable Icon icon;
        private NotificationLevel level = NotificationLevel.INFO;

        private Builder() {
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder subtitle(@Nullable String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder icon(Icon icon) {
            this.icon = icon;
            return this;
        }

        public Builder level(NotificationLevel level) {
            this.level = level;
            return this;
        }

        public Notification build() {
            return new Notification(this);
        }
    }
}
