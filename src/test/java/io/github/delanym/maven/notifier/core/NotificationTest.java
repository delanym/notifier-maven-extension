package io.github.delanym.maven.notifier.core;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URL;

import io.github.delanym.maven.notifier.core.BuildStatus;

class NotificationTest {

    private static final URL ICON_URL = BuildStatus.SUCCESS.iconUrl();

    @Test
    void builderCreatesNotificationWithAllFields() {
        Icon icon = Icon.of(ICON_URL, "test-icon");
        Notification notification = Notification.builder()
                .title("Build Success")
                .message("Built in 42 seconds.")
                .subtitle("Success")
                .icon(icon)
                .level(NotificationLevel.INFO)
                .build();

        Assertions.assertThat(notification.title()).isEqualTo("Build Success");
        Assertions.assertThat(notification.message()).isEqualTo("Built in 42 seconds.");
        Assertions.assertThat(notification.subtitle()).isEqualTo("Success");
        Assertions.assertThat(notification.icon()).isEqualTo(icon);
        Assertions.assertThat(notification.level()).isEqualTo(NotificationLevel.INFO);
    }

    @Test
    void subtitleIsOptional() {
        Notification notification = Notification.builder()
                .title("title")
                .message("message")
                .icon(Icon.of(ICON_URL, "icon"))
                .build();

        Assertions.assertThat(notification.subtitle()).isNull();
    }

    @Test
    void defaultLevelIsInfo() {
        Notification notification = Notification.builder()
                .title("title")
                .message("message")
                .icon(Icon.of(ICON_URL, "icon"))
                .build();

        Assertions.assertThat(notification.level()).isEqualTo(NotificationLevel.INFO);
    }

    @Test
    void builderRequiresTitle() {
        Assertions.assertThatThrownBy(() -> Notification.builder()
                .message("message")
                .icon(Icon.of(ICON_URL, "icon"))
                .build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void equalNotificationsAreEqual() {
        Icon icon = Icon.of(ICON_URL, "icon");
        Notification a = Notification.builder()
                .title("t").message("m").icon(icon).build();
        Notification b = Notification.builder()
                .title("t").message("m").icon(icon).build();

        Assertions.assertThat(a).isEqualTo(b);
        Assertions.assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
