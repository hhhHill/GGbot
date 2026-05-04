package org.example.ggbot.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.example.ggbot.agent.graph.AgentGraphProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AgentGraphPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class
            ))
            .withUserConfiguration(TestConfig.class);

    @Test
    void shouldExposeLegacyDefaults() {
        AgentGraphProperties properties = new AgentGraphProperties();

        assertThat(properties.getMaxIterations()).isEqualTo(10);
    }

    @Test
    void shouldBindRunnerProperties() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "agent.runner.max-iterations", "23"
        ));

        AgentGraphProperties properties = new Binder(source)
                .bind("agent.runner", Bindable.of(AgentGraphProperties.class))
                .orElseThrow(() -> new AssertionError("Expected agent.runner properties to bind"));

        assertThat(properties.getMaxIterations()).isEqualTo(23);
    }

    @Test
    void shouldFailContextStartupWhenMaxIterationsIsInvalid() {
        contextRunner
                .withPropertyValues("agent.runner.max-iterations=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause()
                            .hasMessageContaining("maxIterations")
                            .hasMessageContaining("1");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AgentGraphProperties.class)
    static class TestConfig {
    }
}
