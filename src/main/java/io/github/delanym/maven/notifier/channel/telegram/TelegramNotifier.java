package io.github.delanym.maven.notifier.channel.telegram;

import io.github.delanym.maven.notifier.core.HttpNotifier;
import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.NotificationLevel;
import org.jspecify.annotations.NullMarked;

import java.net.URI;
import java.net.http.HttpClient;

/**
 * Sends build notifications to a Telegram chat via the Bot API.
 *
 * <p>Configuration properties:</p>
 * <ul>
 *   <li>{@code notifier.telegram.token} — the bot token from BotFather (required)</li>
 *   <li>{@code notifier.telegram.chatId} — the target chat/channel ID (required)</li>
 * </ul>
 */
@NullMarked
public final class TelegramNotifier extends HttpNotifier {

    private static final String API_BASE = "https://api.telegram.org/bot";

    private final String token;
    private final String chatId;
    private final String apiBase;

    public TelegramNotifier(String token, String chatId) {
        this.token = token;
        this.chatId = chatId;
        this.apiBase = API_BASE;
    }

    public TelegramNotifier(String token, String chatId, HttpClient httpClient) {
        super(httpClient);
        this.token = token;
        this.chatId = chatId;
        this.apiBase = API_BASE;
    }

    public TelegramNotifier(
            String token,
            String chatId,
            HttpClient httpClient,
            URI baseUri) {
        super(httpClient);
        this.token = token;
        this.chatId = chatId;
        this.apiBase = baseUri.toString();
    }

    @Override
    public String name() {
        return "telegram";
    }

    @Override
    public void send(Notification notification) {
        URI uri = URI.create(apiBase + token + "/sendMessage");
        String emoji = emojiFor(notification.level());
        String text = emoji + " *" + escapeMarkdown(notification.title()) + "*\n"
                + escapeMarkdown(notification.message());
        String json = """
                {
                  "chat_id": "%s",
                  "text": "%s",
                  "parse_mode": "Markdown"
                }""".formatted(chatId, escapeJson(text));
        postJson(uri, json);
    }

    private String emojiFor(NotificationLevel level) {
        return switch (level) {
            case INFO -> "\u2705";
            case WARNING -> "\u26A0\uFE0F";
            case ERROR -> "\u274C";
        };
    }

    private String escapeMarkdown(String text) {
        return text
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("`", "\\`");
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
