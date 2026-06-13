package io.github.delanym.maven.notifier.core;

import org.jspecify.annotations.NullMarked;

import java.util.Objects;
import java.util.Properties;

/**
 * Holds the parsed notifier configuration: which notifier(s) to use,
 * display preferences, build-duration threshold, and pass-through properties
 * for individual notifier implementations.
 */
@NullMarked
public final class NotifierConfiguration {

    private final String implementation;
    private final int thresholdSeconds;
    private final long timeoutMs;
    private final Properties notifierProperties;

    private NotifierConfiguration(Builder builder) {
        this.implementation = Objects.requireNonNull(builder.implementation);
        this.thresholdSeconds = builder.thresholdSeconds;
        this.timeoutMs = builder.timeoutMs;
        this.notifierProperties = new Properties();
        this.notifierProperties.putAll(builder.notifierProperties);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String implementation() {
        return implementation;
    }

    public int thresholdSeconds() {
        return thresholdSeconds;
    }

    public long timeoutMs() {
        return timeoutMs;
    }

    public Properties notifierProperties() {
        Properties copy = new Properties();
        copy.putAll(notifierProperties);
        return copy;
    }

    @Override
    public String toString() {
        return "NotifierConfiguration{implementation='" + implementation
                + "', thresholdSeconds=" + thresholdSeconds
                + ", timeoutMs=" + timeoutMs + "}";
    }

    public static final class Builder {

        private String implementation = "auto";
        private int thresholdSeconds = -1;
        private long timeoutMs = -1;
        private Properties notifierProperties = new Properties();

        private Builder() {
        }

        public Builder implementation(String implementation) {
            this.implementation = implementation;
            return this;
        }

        public Builder thresholdSeconds(int thresholdSeconds) {
            this.thresholdSeconds = thresholdSeconds;
            return this;
        }

        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder notifierProperties(Properties notifierProperties) {
            this.notifierProperties = notifierProperties;
            return this;
        }

        public NotifierConfiguration build() {
            return new NotifierConfiguration(this);
        }
    }
}
