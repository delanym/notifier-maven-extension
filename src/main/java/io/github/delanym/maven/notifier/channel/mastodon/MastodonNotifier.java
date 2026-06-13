package io.github.delanym.maven.notifier.channel.mastodon;

import io.github.delanym.maven.notifier.core.HttpNotifier;
import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.NotificationLevel;
import io.github.delanym.maven.notifier.core.NotifierException;
import org.jspecify.annotations.NullMarked;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Posts build notifications as toots to a Mastodon (or compatible) instance
 * via the statuses API.
 *
 * <p>Configuration properties:</p>
 * <ul>
 *   <li>{@code notifier.mastodon.instanceUrl} — the instance base URL, e.g. https://mastodon.social (required)</li>
 *   <li>{@code notifier.mastodon.accessToken} — OAuth access token with write:statuses scope (required)</li>
 *   <li>{@code notifier.mastodon.visibility} — post visibility: public, unlisted, private, direct (default: unlisted)</li>
 * </ul>
 */
@NullMarked
public final class MastodonNotifier extends HttpNotifier {

    private final URI instanceUrl;
    private final String accessToken;
    private final String visibility;

    public MastodonNotifier(String instanceUrl, String accessToken, String visibility) {
        this.instanceUrl = URI.create(instanceUrl);
        this.accessToken = accessToken;
        this.visibility = visibility;
    }

    public MastodonNotifier(
            String instanceUrl,
            String accessToken,
            String visibility,
            HttpClient httpClient) {
        super(httpClient);
        this.instanceUrl = URI.create(instanceUrl);
        this.accessToken = accessToken;
        this.visibility = visibility;
    }

    @Override
    public String name() {
        return "mastodon";
    }

    @Override
    public void send(Notification notification) {
        URI statusesUri = instanceUrl.resolve("/api/v1/statuses");
        String emoji = emojiFor(notification.level());
        String status = emoji + " " + notification.title() + "\n" + notification.message();

        String formBody = "status=" + urlEncode(status)
                + "&visibility=" + urlEncode(visibility);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(statusesUri)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .timeout(Duration.ofSeconds(10))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new NotifierException(
                        "Mastodon post failed (HTTP " + response.statusCode() + "): " + response.body()
                );
            }
        } catch (java.io.IOException e) {
            throw new NotifierException(
                    "Mastodon notification failed: " + e.getMessage(), e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotifierException("Mastodon notification interrupted", e);
        }
    }

    private String emojiFor(NotificationLevel level) {
        return switch (level) {
            case INFO -> "\u2705";
            case WARNING -> "\u26A0\uFE0F";
            case ERROR -> "\u274C";
        };
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
