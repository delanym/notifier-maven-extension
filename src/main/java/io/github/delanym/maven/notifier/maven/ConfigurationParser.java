package io.github.delanym.maven.notifier.maven;

import io.github.delanym.maven.notifier.core.NotifierConfiguration;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Reads notifier configuration from multiple sources, in increasing
 * priority order:
 * <ol>
 *   <li>Classpath defaults ({@code notifier.properties} in the JAR)</li>
 *   <li>User file ({@code ~/.m2/notifier.properties})</li>
 *   <li>System properties prefixed with {@code notifier.}</li>
 *   <li>The {@code notifyWith} system property (overrides implementation)</li>
 * </ol>
 */
@NullMarked
public final class ConfigurationParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationParser.class);
    private static final String USER_CONFIG_FILE = "notifier.properties";
    private static final String CLASSPATH_CONFIG = "notifier.properties";

    public NotifierConfiguration parse() {
        Properties properties = new Properties();

        loadFromClasspath(properties);
        loadFromUserHome(properties);
        loadFromSystemProperties(properties);
        applyOverride(properties);

        LOGGER.debug("Notifier configuration properties: {}", properties);
        return toConfiguration(properties);
    }

    private void loadFromClasspath(Properties target) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CLASSPATH_CONFIG)) {
            if (in != null) {
                target.load(in);
            }
        } catch (IOException e) {
            LOGGER.debug("Could not load classpath config: {}", e.getMessage());
        }
    }

    private void loadFromUserHome(Properties target) {
        Path userConfig = Path.of(System.getProperty("user.home"), ".m2", USER_CONFIG_FILE);
        if (Files.exists(userConfig)) {
            try (InputStream in = Files.newInputStream(userConfig)) {
                target.load(in);
                LOGGER.debug("Loaded user config from {}", userConfig);
            } catch (IOException e) {
                LOGGER.debug("Could not load user config from {}: {}", userConfig, e.getMessage());
            }
        }
    }

    private void loadFromSystemProperties(Properties target) {
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = entry.getKey().toString();
            if (key.startsWith("notifier.")) {
                target.put(key, entry.getValue());
            }
        }
    }

    private void applyOverride(Properties target) {
        String override = System.getProperty("notifyWith");
        if (override != null) {
            LOGGER.debug("Overriding implementation with: {}", override);
            target.put("notifier.implementation", override);
        }
    }

    private NotifierConfiguration toConfiguration(Properties props) {
        return NotifierConfiguration.builder()
                .implementation(props.getProperty("notifier.implementation", "auto"))
                .thresholdSeconds(Integer.parseInt(props.getProperty("notifier.threshold", "-1")))
                .timeoutMs(Long.parseLong(props.getProperty("notifier.timeout", "-1")))
                .notifierProperties(props)
                .build();
    }
}
