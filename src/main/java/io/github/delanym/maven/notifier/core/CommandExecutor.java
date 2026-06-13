package io.github.delanym.maven.notifier.core;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Executes external commands (e.g. notify-send, terminal-notifier) with a
 * configurable timeout. Used by desktop notifiers that shell out to OS-native
 * notification tools.
 */
@NullMarked
public final class CommandExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandExecutor.class);
    private static final long DEFAULT_TIMEOUT_MS = 5000;

    private final long timeoutMs;

    public CommandExecutor() {
        this(DEFAULT_TIMEOUT_MS);
    }

    public CommandExecutor(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * Executes the command and waits up to the configured timeout for
     * completion. Returns the exit code, or -1 if the process times out.
     */
    public int execute(String... command) {
        LOGGER.debug("Executing: {}", String.join(" ", command));
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOGGER.warn("Command timed out after {}ms: {}", timeoutMs, command[0]);
                return -1;
            }
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String output = new String(process.getInputStream().readAllBytes());
                LOGGER.warn("Command exited with {}: {} — {}", exitCode, command[0], output);
            }
            return exitCode;
        } catch (IOException e) {
            LOGGER.debug("Command not found or not executable: {}", command[0]);
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    /**
     * Probes whether a command is available on the system by running it and
     * checking the exit code. Commands that exit with 0 are considered
     * available.
     */
    public boolean isCommandAvailable(String... command) {
        return execute(command) == 0;
    }
}
