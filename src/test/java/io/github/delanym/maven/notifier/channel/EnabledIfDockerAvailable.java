package io.github.delanym.maven.notifier.channel;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit 5 condition that skips a test class when Docker is not available,
 * instead of failing with an {@link IllegalStateException}. Apply this
 * alongside {@code @Testcontainers} on integration tests that require
 * Docker.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledIfDockerAvailable.Condition.class)
@interface EnabledIfDockerAvailable {

    class Condition implements ExecutionCondition {

        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            try {
                DockerClientFactory.instance().client();
                return ConditionEvaluationResult.enabled("Docker is available");
            } catch (Exception e) {
                return ConditionEvaluationResult.disabled("Docker is not available: " + e.getMessage());
            }
        }
    }
}
