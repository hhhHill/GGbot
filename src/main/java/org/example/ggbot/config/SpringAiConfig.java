package org.example.ggbot.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.concurrent.Executor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Spring AI 基础设施接入层。
 *
 * <p>当前阶段只完成依赖与基础 Bean 接入，不接管现有 Agent 主链路。
 * 这样项目可以先具备 Spring AI 的运行基础，再逐步替换自定义 llm/tool/memory 抽象。
 */
@Configuration
@Data
@RequiredArgsConstructor
public class SpringAiConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    @Bean
    public Executor llmProbeExecutor() {
        return new SimpleAsyncTaskExecutor("llm-probe-");
    }
}
