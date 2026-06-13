package io.github.delanym.maven.notifier.channel;

import io.github.delanym.maven.notifier.core.BuildStatus;
import io.github.delanym.maven.notifier.core.Icon;
import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.NotificationLevel;
import io.github.delanym.maven.notifier.channel.discord.DiscordNotifier;
import io.github.delanym.maven.notifier.channel.mastodon.MastodonNotifier;
import io.github.delanym.maven.notifier.channel.slack.SlackNotifier;
import io.github.delanym.maven.notifier.channel.teams.TeamsNotifier;
import io.github.delanym.maven.notifier.channel.telegram.TelegramNotifier;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;
import java.net.http.HttpClient;

/**
 * Integration tests for all HTTP-webhook-based notifiers. Uses WireMock
 * to stand up a local HTTP server that validates the requests each notifier
 * sends.
 */
class WebhookNotifierIT {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void slackSendsAttachmentWithCorrectColor() {
        wireMock.stubFor(WireMock.post("/slack-webhook")
                .willReturn(WireMock.aResponse().withStatus(200)));

        SlackNotifier notifier = new SlackNotifier(
                wireMock.baseUrl() + "/slack-webhook",
                httpClient
        );

        notifier.send(successNotification());

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/slack-webhook"))
                .withRequestBody(WireMock.containing("\"color\": \"good\""))
                .withRequestBody(WireMock.containing("my-project")));
    }

    @Test
    void slackSendsDangerColorOnFailure() {
        wireMock.stubFor(WireMock.post("/slack-webhook")
                .willReturn(WireMock.aResponse().withStatus(200)));

        SlackNotifier notifier = new SlackNotifier(
                wireMock.baseUrl() + "/slack-webhook",
                httpClient
        );

        notifier.send(failureNotification());

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/slack-webhook"))
                .withRequestBody(WireMock.containing("\"color\": \"danger\"")));
    }

    @Test
    void discordSendsEmbedWithTitle() {
        wireMock.stubFor(WireMock.post("/discord-webhook")
                .willReturn(WireMock.aResponse().withStatus(204)));

        DiscordNotifier notifier = new DiscordNotifier(
                wireMock.baseUrl() + "/discord-webhook",
                httpClient
        );

        notifier.send(successNotification());

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/discord-webhook"))
                .withRequestBody(WireMock.containing("\"title\": \"my-project\""))
                .withRequestBody(WireMock.containing("embeds")));
    }

    @Test
    void teamsSendsAdaptiveCard() {
        wireMock.stubFor(WireMock.post("/teams-webhook")
                .willReturn(WireMock.aResponse().withStatus(200)));

        TeamsNotifier notifier = new TeamsNotifier(
                wireMock.baseUrl() + "/teams-webhook",
                httpClient
        );

        notifier.send(successNotification());

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/teams-webhook"))
                .withRequestBody(WireMock.containing("AdaptiveCard"))
                .withRequestBody(WireMock.containing("my-project")));
    }

    @Test
    void telegramSendsMessageWithMarkdown() {
        wireMock.stubFor(WireMock.post(WireMock.urlPathMatching("/bot.*/sendMessage"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("{\"ok\":true,\"result\":{}}")));

        TelegramNotifier notifier = new TelegramNotifier(
                "test-token",
                "12345",
                httpClient,
                URI.create(wireMock.baseUrl() + "/bot")
        );

        notifier.send(successNotification());

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlPathMatching("/bottest-token/sendMessage"))
                .withRequestBody(WireMock.containing("\"chat_id\": \"12345\""))
                .withRequestBody(WireMock.containing("my-project")));
    }

    @Test
    void mastodonPostsStatusWithBearerToken() {
        wireMock.stubFor(WireMock.post("/api/v1/statuses")
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withBody("{\"id\":\"1\"}")));

        MastodonNotifier notifier = new MastodonNotifier(
                wireMock.baseUrl(),
                "test-access-token",
                "unlisted",
                httpClient
        );

        notifier.send(successNotification());

        wireMock.verify(WireMock.postRequestedFor(WireMock.urlEqualTo("/api/v1/statuses"))
                .withHeader("Authorization", WireMock.equalTo("Bearer test-access-token"))
                .withRequestBody(WireMock.containing("my-project")));
    }

    private Notification successNotification() {
        return Notification.builder()
                .title("my-project")
                .message("Built in 10 second(s).")
                .subtitle("Success")
                .icon(Icon.of(BuildStatus.SUCCESS.iconUrl(), "success"))
                .level(NotificationLevel.INFO)
                .build();
    }

    private Notification failureNotification() {
        return Notification.builder()
                .title("my-project")
                .message("Build Failed.")
                .subtitle("Failure")
                .icon(Icon.of(BuildStatus.FAILURE.iconUrl(), "failure"))
                .level(NotificationLevel.ERROR)
                .build();
    }
}
