package io.github.delanym.maven.notifier.channel.slack;

import io.github.delanym.maven.notifier.core.HttpNotifier;
import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.NotificationLevel;
import org.jspecify.annotations.NullMarked;

import java.net.URI;
import java.net.http.HttpClient;

/**
 * Sends build notifications to a Slack channel via an incoming webhook URL.
 * Messages are formatted as Slack Block Kit attachments with color-coded
 * sidebars indicating build status.
 *
 * <p>Configuration properties:</p>
 * <ul>
 *   <li>{@code notifier.slack.webhook} — the incoming webhook URL (required)</li>
 * </ul>
 */
@NullMarked
public final class SlackNotifier extends HttpNotifier {

    private final URI webhookUri;

    public SlackNotifier(String webhookUrl) {
        this.webhookUri = URI.create(webhookUrl);
    }

    public SlackNotifier(String webhookUrl, HttpClient httpClient) {
        super(httpClient);
        this.webhookUri = URI.create(webhookUrl);
    }

    @Override
    public String name() {
        return "slack";
    }

    @Override
    public void send(Notification notification) {
        String json = buildPayload(notification);
        postJson(webhookUri, json);
    }

    private String buildPayload(Notification notification) {
        String color = colorFor(notification.level());
        String subtitle = notification.subtitle() != null ? notification.subtitle() : "";
        return """
                {
                  "attachments": [
                    {
                      "color": "%s",
                      "title": "%s",
                      "text": "%s",
                      "footer": "%s"
                    }
                  ]
                }""".formatted(
                color,
                escapeJson(notification.title()),
                escapeJson(notification.message()),
                escapeJson(subtitle)
        );
    }

    private String colorFor(NotificationLevel level) {
        return switch (level) {
            case INFO -> "good";
            case WARNING -> "warning";
            case ERROR -> "danger";
        };
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
