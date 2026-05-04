package org.example.ggbot.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ClasspathPromptRepositoryTest {

    @Test
    void shouldLoadPromptTextFromClasspath() {
        ClasspathPromptRepository repository = new ClasspathPromptRepository();

        String prompt = repository.load("summarize-system-prompt.txt");

        assertThat(prompt).contains("你是 GGbot 的 Web 对话助手，负责为用户提供清晰、准确、可执行的回答。");
        assertThat(prompt).contains("# Memory Awareness（关键新增）");
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
