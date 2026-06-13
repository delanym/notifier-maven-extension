package io.github.delanym.maven.notifier.maven;

import io.github.delanym.maven.notifier.core.NotifierConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class ConfigurationParserTest {

    @Test
    void defaultConfigurationUsesAutoImplementation() {
        ConfigurationParser parser = new ConfigurationParser();

        NotifierConfiguration config = parser.parse();

        Assertions.assertThat(config.implementation()).isEqualTo("auto");
        Assertions.assertThat(config.thresholdSeconds()).isEqualTo(-1);
    }

    @Test
    void systemPropertyOverridesImplementation() {
        System.setProperty("notifyWith", "slack");
        try {
            ConfigurationParser parser = new ConfigurationParser();
            NotifierConfiguration config = parser.parse();
            Assertions.assertThat(config.implementation()).isEqualTo("slack");
        } finally {
            System.clearProperty("notifyWith");
        }
    }

    @Test
    void notifierPropertiesPassedThrough() {
        System.setProperty("notifier.slack.webhook", "https://hooks.example.com/test");
        try {
            ConfigurationParser parser = new ConfigurationParser();
            NotifierConfiguration config = parser.parse();
            Assertions.assertThat(config.notifierProperties().getProperty("notifier.slack.webhook"))
                    .isEqualTo("https://hooks.example.com/test");
        } finally {
            System.clearProperty("notifier.slack.webhook");
        }
    }
}
