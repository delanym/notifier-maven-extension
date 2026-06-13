package io.github.delanym.maven.notifier.core;

/**
 * Thrown when a notification channel encounters an unrecoverable error
 * during initialization or notify.
 */
public class NotifierException extends RuntimeException {

    public NotifierException(String message) {
        super(message);
    }

    public NotifierException(String message, Throwable cause) {
        super(message, cause);
    }
}
