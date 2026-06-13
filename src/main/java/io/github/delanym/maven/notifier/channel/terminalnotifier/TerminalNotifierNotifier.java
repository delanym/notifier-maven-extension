package io.github.delanym.maven.notifier.channel.terminalnotifier;

import io.github.delanym.maven.notifier.core.CommandExecutor;
import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.Notifier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * macOS desktop notifications via the {@code terminal-notifier} command-line
 * tool. Supports title, subtitle, message, icon, sound, and activation of a
 * specific application on click.
 *
 * @see <a href="https://github.com/julienXX/terminal-notifier">terminal-notifier</a>
 */
@NullMarked
public final class TerminalNotifierNotifier implements Notifier {

    private final CommandExecutor executor;
    private final String binary;
    private final @Nullable String activateApplication;
    private final @Nullable String sound;

    public TerminalNotifierNotifier(
            CommandExecutor executor,
            String binary,
            @Nullable String activateApplication,
            @Nullable String sound) {
        this.executor = executor;
        this.binary = binary;
        this.activateApplication = activateApplication;
        this.sound = sound;
    }

    @Override
    public void close() {
    }

    @Override
    public void init() {
    }

    @Override
    public boolean isAvailable() {
        return executor.isCommandAvailable(binary, "--help");
    }

    @Override
    public String name() {
        return "terminal-notifier";
    }

    @Override
    public void send(Notification notification) {
        List<String> command = new ArrayList<>();
        command.add(binary);
        command.add("-title");
        command.add(notification.title());
        command.add("-message");
        command.add(notification.message());

        if (notification.subtitle() != null) {
            command.add("-subtitle");
            command.add(notification.subtitle());
        }

        command.add("-contentImage");
        command.add(notification.icon().asPath().toString());

        if (activateApplication != null) {
            command.add("-activate");
            command.add(activateApplication);
        }

        command.add("-group");
        command.add("com.github.delanym.notifier");

        if (sound != null) {
            command.add("-sound");
            command.add(sound);
        }

        executor.execute(command.toArray(String[]::new));
    }
}
