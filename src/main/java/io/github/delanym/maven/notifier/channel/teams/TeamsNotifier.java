package io.github.delanym.maven.notifier.channel.teams;

import io.github.delanym.maven.notifier.core.HttpNotifier;
import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.NotificationLevel;
import org.jspecify.annotations.NullMarked;

import java.net.URI;
import java.net.http.HttpClient;

/**
 * Sends build notifications to Microsoft Teams via an incoming webhook
 * connector. Uses the Adaptive Card format for rich message rendering.
 *
 * <p>Configuration properties:</p>
 * <ul>
 *   <li>{@code notifier.teams.webhook} — the incoming webhook URL (required)</li>
 * </ul>
 */
@NullMarked
public final class TeamsNotifier extends HttpNotifier {

    private final URI webhookUri;

    public TeamsNotifier(String webhookUrl) {
        this.webhookUri = URI.create(webhookUrl);
    }

    public TeamsNotifier(String webhookUrl, HttpClient httpClient) {
        super(httpClient);
        this.webhookUri = URI.create(webhookUrl);
    }

    @Override
    public String name() {
        return "teams";
    }

    @Override
    public void send(Notification notification) {
        String json = buildAdaptiveCard(notification);
        postJson(webhookUri, json);
    }

    private String buildAdaptiveCard(Notification notification) {
        String color = colorFor(notification.level());
        return """
                {
                  "type": "message",
                  "attachments": [
                    {
                      "contentType": "application/vnd.microsoft.card.adaptive",
                      "content": {
                        "$schema": "http://adaptivecards.io/schemas/adaptive-card.json",
                        "type": "AdaptiveCard",
                        "version": "1.4",
                        "body": [
                          {
                            "type": "TextBlock",
                            "text": "%s",
                            "weight": "Bolder",
                            "size": "Medium",
                            "color": "%s"
                          },
                          {
                            "type": "TextBlock",
                            "text": "%s",
                            "wrap": true
                          }
                        ]
                      }
                    }
                  ]
                }""".formatted(
                escapeJson(notification.title()),
                color,
                escapeJson(notification.message())
        );
    }

    private String colorFor(NotificationLevel level) {
        return switch (level) {
            case INFO -> "Good";
            case WARNING -> "Warning";
            case ERROR -> "Attention";
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
