package io.github.delanym.maven.notifier.core;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Base class for notifiers that post to an HTTP webhook or REST API.
 * Provides a shared {@link HttpClient} with sensible defaults and a
 * convenience method for JSON POST requests.
 */
@NullMarked
public abstract class HttpNotifier implements Notifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpNotifier.class);

    protected final HttpClient httpClient;

    protected HttpNotifier() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    protected HttpNotifier(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Sends a JSON POST request to the given URI and returns the response body.
     *
     * @throws NotifierException if the request fails or returns a non-2xx status
     */
    protected String postJson(URI uri, String jsonBody) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(10))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new NotifierException(
                        name() + " notification failed (HTTP " + response.statusCode() + "): " + response.body()
                );
            }
            return response.body();
        } catch (IOException e) {
            throw new NotifierException(name() + " notification failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NotifierException(name() + " notification interrupted", e);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void init() {
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}
