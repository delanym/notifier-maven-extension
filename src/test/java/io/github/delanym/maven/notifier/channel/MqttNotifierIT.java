package io.github.delanym.maven.notifier.channel;

import io.github.delanym.maven.notifier.core.BuildStatus;
import io.github.delanym.maven.notifier.core.Icon;
import io.github.delanym.maven.notifier.core.Notification;
import io.github.delanym.maven.notifier.core.NotificationLevel;
import io.github.delanym.maven.notifier.channel.mqtt.MqttNotifier;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;

/**
 * Integration test for {@link MqttNotifier} using a real Mosquitto MQTT
 * broker running in a Testcontainer. Verifies that messages published by the
 * notifier can be received by a subscriber on the same topic.
 */
@Testcontainers
@EnabledIfDockerAvailable
class MqttNotifierIT {

    private static final String TOPIC = "notifier/test-builds";
    private static final int MQTT_PORT = 1883;

    @Container
    static final GenericContainer<?> mosquitto = new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:2"))
            .withExposedPorts(MQTT_PORT)
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("mosquitto-no-auth.conf"),
                    "/mosquitto/config/mosquitto.conf"
            )
            .waitingFor(Wait.forListeningPort());

    private MqttNotifier notifier;
    private Mqtt5BlockingClient subscriber;

    @BeforeEach
    void setUp() {
        String host = mosquitto.getHost();
        int port = mosquitto.getMappedPort(MQTT_PORT);
        String brokerUrl = "tcp://" + host + ":" + port;

        notifier = new MqttNotifier(brokerUrl, TOPIC, "notifier-test-publisher", null, null);
        notifier.init();

        subscriber = MqttClient.builder()
                .useMqttVersion5()
                .identifier("notifier-test-subscriber")
                .serverHost(host)
                .serverPort(port)
                .buildBlocking();
        subscriber.connect();
    }

    @AfterEach
    void tearDown() {
        notifier.close();
        if (subscriber != null) {
            try {
                subscriber.disconnect();
            } catch (Exception ignored) {
                // best-effort disconnect
            }
        }
    }

    @Test
    void publishedMessageIsReceivedBySubscriber() throws Exception {
      CountDownLatch latch = new CountDownLatch(1);
      AtomicReference<String> receivedPayload = new AtomicReference<>();

      subscriber.toAsync().subscribeWith()
          .topicFilter(TOPIC)
          .qos(MqttQos.AT_LEAST_ONCE)
          .callback(publish -> {
            receivedPayload.set(new String(publish.getPayloadAsBytes(), StandardCharsets.UTF_8));
            latch.countDown();
          })
          .send()
          .get(5, TimeUnit.SECONDS);

      notifier.send(buildNotification());

      Assertions.assertThat(latch.await(5, TimeUnit.SECONDS))
          .as("MQTT message should be received within 5 seconds")
          .isTrue();

      String payload = receivedPayload.get();
      Assertions.assertThat(payload)
          .contains("\"title\": \"my-project\"")
          .contains("\"level\": \"INFO\"");
    }

    private Notification buildNotification() {
        return Notification.builder()
                .title("my-project")
                .message("Built in 42 second(s).")
                .icon(Icon.of(BuildStatus.SUCCESS.iconUrl(), "success"))
                .level(NotificationLevel.INFO)
                .build();
    }
}
