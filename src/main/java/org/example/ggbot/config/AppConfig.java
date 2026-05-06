package org.example.ggbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.graph.AgentGraphProperties;
import org.example.ggbot.adapter.feishu.FeishuProperties;
import org.example.ggbot.tool.impl.ModifyPptTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

@Configuration
@Data
@RequiredArgsConstructor
@EnableConfigurationProperties({FeishuProperties.class, AgentGraphProperties.class})
public class AppConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            ModifyPptTool modifyPptTool) {
        // Only register tools that do not depend on ChatClient themselves.
        // GenerateDocTool / GeneratePptTool both depend on chat generation and
        // registering them globally would create a startup cycle in Spring AI tool resolution.
        return MethodToolCallbackProvider.builder()
                .toolObjects(modifyPptTool)
                .build()
                ;
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    public java.util.concurrent.Executor jobWorkerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ggbot-agent-task-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
