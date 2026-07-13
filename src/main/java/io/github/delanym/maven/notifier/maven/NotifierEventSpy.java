package io.github.delanym.maven.notifier.maven;

import io.github.delanym.maven.notifier.core.BuildStatus;
import io.github.delanym.maven.notifier.core.NotifierConfiguration;
import io.github.delanym.maven.notifier.core.DoNothingNotifier;
import io.github.delanym.maven.notifier.core.Icon;
import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.NotificationLevel;
import io.github.delanym.maven.notifier.core.Notifier;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenExecutionResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;

/**
 * Maven {@link org.apache.maven.eventspy.EventSpy} that sends build
 * notifications through the configured notification channel(s). Registered
 * via JSR-330 / Sisu discovery.
 *
 * <p>Notifications include the project name, build outcome, duration,
 * and git branch (with dirty-state indicator).</p>
 */
@Named("notifier")
@Singleton
@NullMarked
public final class NotifierEventSpy extends AbstractEventSpy {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotifierEventSpy.class);
    private static final String LINE_BREAK = System.lineSeparator();
    private static final int MAX_TITLE_LENGTH = 40;
    private static final int MAX_MESSAGE_LENGTH = 1000;

    private Notifier notifier = DoNothingNotifier.instance();
    private NotifierConfiguration configuration = NotifierConfiguration.builder().build();
    private Instant buildStart = Instant.now();
    private @Nullable BuildContext buildContext;

    @Override
    public void close() throws Exception {
        notifier.close();
    }

    @Override
    public void init(Context context) throws Exception {
        LOGGER.debug("Notifier Maven Extension initializing");
        buildStart = Instant.now();

        ConfigurationParser parser = new ConfigurationParser();
        configuration = parser.parse();
        LOGGER.debug("Configuration: {}", configuration);

        if ("true".equalsIgnoreCase(System.getProperty("skipNotification"))) {
            LOGGER.debug("Notifications disabled via -DskipNotification");
            notifier = DoNothingNotifier.instance();
            return;
        }

        NotifierRegistry registry = new NotifierRegistry();
        notifier = registry.resolve(configuration);
        notifier.init();
        LOGGER.debug("Active notifier: {}", notifier.name());
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (event instanceof ExecutionEvent execEvent
                && execEvent.getType() == ExecutionEvent.Type.SessionStarted) {
            buildContext = BuildContext.from(execEvent.getSession());
            LOGGER.debug("Captured build context: goals={}, branch={}",
                    buildContext.goals(), buildContext.gitBranch());
            return;
        }

        if (!(event instanceof MavenExecutionResult result)) {
            return;
        }

        long elapsedSeconds = Duration.between(buildStart, Instant.now()).toSeconds();
        int threshold = configuration.thresholdSeconds();
        if (threshold >= 0 && elapsedSeconds <= threshold && !notifier.isPersistent()) {
            LOGGER.debug("Build completed in {}s, below threshold of {}s; skipping notification",
                    elapsedSeconds, threshold);
            return;
        }

        if (result.getProject() == null && result.hasExceptions()) {
            sendErrorNotification(result);
            return;
        }

        BuildStatus status = result.hasExceptions() ? BuildStatus.FAILURE : BuildStatus.SUCCESS;
        String title = buildTitle(result, status, elapsedSeconds);
        String message = buildMessage(result, status, elapsedSeconds);

        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .subtitle(status.message())
                .icon(Icon.of(status.iconUrl(), status.name()))
                .level(toLevelFrom(status))
                .build();

        notifier.send(notification);
    }

    private void sendErrorNotification(MavenExecutionResult result) {
        StringBuilder message = new StringBuilder();
        for (Throwable exception : result.getExceptions()) {
            message.append(exception.getMessage()).append(LINE_BREAK);
        }
        Notification notification = Notification.builder()
                .title(truncate("Maven Build Error", MAX_TITLE_LENGTH))
                .message(truncate(message.toString(), MAX_MESSAGE_LENGTH))
                .subtitle(BuildStatus.FAILURE.message())
                .icon(Icon.of(BuildStatus.FAILURE.iconUrl(), BuildStatus.FAILURE.name()))
                .level(NotificationLevel.ERROR)
                .build();
        notifier.send(notification);
    }

    private String buildTitle(
            MavenExecutionResult result,
            BuildStatus status,
            long elapsedSeconds) {
        return truncate(
                "Maven " + status.message()
                        + " [" + elapsedSeconds + "s] "
                        + result.getProject().getName(),
                MAX_TITLE_LENGTH);
    }

    private String buildMessage(
            MavenExecutionResult result,
            BuildStatus status,
            long elapsedSeconds) {
        BuildContext ctx = this.buildContext;
        if (ctx != null && ctx.gitBranch() != null) {
            String branch = "on " + ctx.gitBranch();
            if (ctx.gitDirty()) {
                branch += "*";
            }
            return truncate(branch, MAX_MESSAGE_LENGTH);
        }
        return "";
    }

    private NotificationLevel toLevelFrom(BuildStatus status) {
        return switch (status) {
            case SUCCESS -> NotificationLevel.INFO;
            case FAILURE -> NotificationLevel.ERROR;
        };
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "\u2026";
    }
}
