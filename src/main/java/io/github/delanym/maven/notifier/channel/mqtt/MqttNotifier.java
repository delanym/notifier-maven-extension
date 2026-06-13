package io.github.delanym.maven.notifier.channel.mqtt;

import io.github.delanym.maven.notifier.core.NotifierException;
import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.Notifier;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Publishes build notifications as MQTT v5 messages to a topic on a Mosquitto
 * (or any MQTT v5 compatible) broker. Messages are published with QoS 1
 * (at least once delivery).
 *
 * <p>Configuration properties:</p>
 * <ul>
 *   <li>{@code notifier.mqtt.brokerUrl} — broker URL, e.g. tcp://localhost:1883 (required)</li>
 *   <li>{@code notifier.mqtt.topic} — MQTT topic (default: notifier/builds)</li>
 *   <li>{@code notifier.mqtt.clientId} — client ID (default: notifier-maven)</li>
 *   <li>{@code notifier.mqtt.username} — optional username</li>
 *   <li>{@code notifier.mqtt.password} — optional password</li>
 * </ul>
 */
@NullMarked
public final class MqttNotifier implements Notifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttNotifier.class);

    private final String brokerUrl;
    private final String topic;
    private final String clientId;
    private final @Nullable String username;
    private final @Nullable String password;

    private @Nullable Mqtt5BlockingClient client;

    public MqttNotifier(
            String brokerUrl,
            String topic,
            String clientId,
            @Nullable String username,
            @Nullable String password) {
        this.brokerUrl = brokerUrl;
        this.topic = topic;
        this.clientId = clientId;
        this.username = username;
        this.password = password;
    }

    @Override
    public void close() {
        Mqtt5BlockingClient mqttClient = this.client;
        if (mqttClient != null) {
            try {
                mqttClient.disconnect();
            } catch (Exception e) {
                LOGGER.warn("Error disconnecting MQTT client: {}", e.getMessage());
            }
        }
    }

    @Override
    public void init() {
        try {
            String host;
            int port;
            if (brokerUrl.startsWith("tcp://")) {
                String hostPort = brokerUrl.substring("tcp://".length());
                int colonIdx = hostPort.lastIndexOf(':');
                if (colonIdx >= 0) {
                    host = hostPort.substring(0, colonIdx);
                    port = Integer.parseInt(hostPort.substring(colonIdx + 1));
                } else {
                    host = hostPort;
                    port = 1883;
                }
            } else {
                host = brokerUrl;
                port = 1883;
            }

            var builder = MqttClient.builder()
                    .useMqttVersion5()
                    .identifier(clientId)
                    .serverHost(host)
                    .serverPort(port);

            if (username != null || password != null) {
                var authBuilder = builder.simpleAuth();
                if (username != null) {
                    var complete = authBuilder.username(username);
                    if (password != null) {
                        complete.password(password.getBytes(StandardCharsets.UTF_8));
                    }
                    complete.applySimpleAuth();
                } else {
                    // password must be non-null here (due to outer if)
                    authBuilder.password(password.getBytes(StandardCharsets.UTF_8)).applySimpleAuth();
                }
            }

            client = builder.buildBlocking();
            client.connect();
            LOGGER.debug("Connected to MQTT broker at {}", brokerUrl);
        } catch (Exception e) {
            throw new NotifierException("Failed to connect to MQTT broker at " + brokerUrl, e);
        }
    }

    @Override
    public String name() {
        return "mqtt";
    }

    @Override
    public void send(Notification notification) {
        Mqtt5BlockingClient mqttClient = this.client;
        if (mqttClient == null) {
            LOGGER.warn("MQTT client not connected; cannot send notification");
            return;
        }
        String payload = buildPayload(notification);
        try {
            mqttClient.publishWith()
                    .topic(topic)
                    .qos(MqttQos.AT_LEAST_ONCE)
                    .payload(payload.getBytes(StandardCharsets.UTF_8))
                    .send();
            LOGGER.debug("Published to MQTT topic {}: {}", topic, notification.title());
        } catch (Exception e) {
            throw new NotifierException("Failed to publish MQTT message: " + e.getMessage(), e);
        }
    }

    private String buildPayload(Notification notification) {
        return """
                {
                  "title": "%s",
                  "message": "%s",
                  "level": "%s"
                }""".formatted(
                escapeJson(notification.title()),
                escapeJson(notification.message()),
                notification.level().name()
        );
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
