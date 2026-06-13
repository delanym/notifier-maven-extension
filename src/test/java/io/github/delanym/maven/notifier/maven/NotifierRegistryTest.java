package io.github.delanym.maven.notifier.maven;

import io.github.delanym.maven.notifier.core.CompositeNotifier;
import io.github.delanym.maven.notifier.core.NotifierConfiguration;
import io.github.delanym.maven.notifier.core.DoNothingNotifier;
import io.github.delanym.maven.notifier.core.Notifier;
import io.github.delanym.maven.notifier.channel.burnttoast.BurntToastNotifier;
import io.github.delanym.maven.notifier.channel.slack.SlackNotifier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

class NotifierRegistryTest {

    private final NotifierRegistry registry = new NotifierRegistry();

    @Test
    void resolvesBurntToastByName() {
        NotifierConfiguration config = configWith("burnttoast");

        Notifier notifier = registry.resolve(config);

        Assertions.assertThat(notifier).isInstanceOf(BurntToastNotifier.class);
    }

    @Test
    void resolvesSlackByName() {
        Properties props = new Properties();
        props.setProperty("notifier.implementation", "slack");
        props.setProperty("notifier.slack.webhook", "https://hooks.example.com/test");
        NotifierConfiguration config = NotifierConfiguration.builder()
                .implementation("slack")
                .notifierProperties(props)
                .build();

        Notifier notifier = registry.resolve(config);

        Assertions.assertThat(notifier).isInstanceOf(SlackNotifier.class);
    }

    @Test
    void resolvesCompositeForCommaSeparatedList() {
        Properties props = new Properties();
        props.setProperty("notifier.slack.webhook", "https://hooks.example.com/test");
        props.setProperty("notifier.discord.webhook", "https://discord.example.com/test");
        NotifierConfiguration config = NotifierConfiguration.builder()
                .implementation("slack,discord")
                .notifierProperties(props)
                .build();

        Notifier notifier = registry.resolve(config);

        Assertions.assertThat(notifier).isInstanceOf(CompositeNotifier.class);
    }

    @Test
    void unknownNotifierFallsBackToDoNothing() {
        NotifierConfiguration config = configWith("nonexistent");

        Notifier notifier = registry.resolve(config);

        Assertions.assertThat(notifier).isInstanceOf(DoNothingNotifier.class);
    }

    private NotifierConfiguration configWith(String implementation) {
        return NotifierConfiguration.builder()
                .implementation(implementation)
                .build();
    }
}
