package io.github.delanym.maven.notifier.channel.burnttoast;

import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.Notifier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Windows 10/11 desktop notifications via the BurntToast PowerShell module.
 * Executes {@code pwsh} (or {@code powershell}) with {@code -NoProfile} to
 * avoid loading user profile scripts, which dramatically reduces startup time.
 *
 * <p>The PowerShell process is started but not waited on. Because it is an
 * OS-level process, it continues running independently after the Maven JVM
 * exits, so the build is never blocked.</p>
 */
@NullMarked
public final class BurntToastNotifier implements Notifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(BurntToastNotifier.class);

    private final @Nullable String sound;
    private String powershellBinary = "pwsh";

    public BurntToastNotifier(@Nullable String sound) {
        this.sound = sound;
    }

    @Override
    public void close() {
    }

    @Override
    public void init() {
        if (!tryCommand("pwsh")) {
            powershellBinary = "powershell";
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    powershellBinary, "-NoProfile", "-NonInteractive", "-Command",
                    "Get-Module -ListAvailable -Name BurntToast | Format-Table Name"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            String output = new String(process.getInputStream().readAllBytes());
            return process.exitValue() == 0 && output.toLowerCase().contains("burnttoast");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @Override
    public String name() {
        return "burnttoast";
    }

    @Override
    public void send(Notification notification) {
        StringBuilder command = new StringBuilder()
                .append("New-BurntToastNotification -Text '")
                .append(escapeForPowerShell(notification.title()))
                .append("', '")
                .append(escapeForPowerShell(notification.message()))
                .append("' -AppLogo '")
                .append(notification.icon().asPath())
                .append("'");

        if (sound == null) {
            command.append(" -Silent");
        } else {
            command.append(" -Sound ").append(sound);
        }

        String psCommand = command.toString();
        LOGGER.debug("Executing BurntToast: {}", psCommand);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    powershellBinary, "-NoProfile", "-NonInteractive", "-Command", psCommand
            );
            pb.redirectErrorStream(true);
            pb.start();
        } catch (IOException e) {
            LOGGER.warn("Failed to start BurntToast notification process", e);
        }
    }

    private boolean tryCommand(String binary) {
        try {
            ProcessBuilder pb = new ProcessBuilder(binary, "-NoProfile", "-Command", "echo ok");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            return process.waitFor(5, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private String escapeForPowerShell(String text) {
        return text.replace("'", "''");
    }
}
