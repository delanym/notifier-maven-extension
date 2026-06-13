package io.github.delanym.maven.notifier.core;

import java.net.URL;

/**
 * Outcome of a Maven build or module execution, mapped to a display message
 * and an icon resource for notification rendering.
 */
public enum BuildStatus {

    SUCCESS("Success", "emoji_u2705.png"),
    FAILURE("Failure", "emoji_u274c.png");

    private final String message;
    private final String iconResource;

    BuildStatus(String message, String iconResource) {
        this.message = message;
        this.iconResource = iconResource;
    }

    public String message() {
        return message;
    }

    public URL iconUrl() {
        return BuildStatus.class.getClassLoader().getResource(iconResource);
    }
}
