package org.example.ggbot.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ClasspathPromptRepositoryTest {

    @Test
    void shouldLoadPromptTextFromClasspath() {
        ClasspathPromptRepository repository = new ClasspathPromptRepository();

        String prompt = repository.load("summarize-system-prompt.txt");

        assertThat(prompt).contains("你是 GGbot 的 Web 对话助手。");
        assertThat(prompt).doesNotContain("MVP");
    }

    @Test
    void shouldFailFastWhenPromptResourceDoesNotExist() {
        ClasspathPromptRepository repository = new ClasspathPromptRepository();

        assertThatThrownBy(() -> repository.load("missing.txt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing.txt");
    }
}
