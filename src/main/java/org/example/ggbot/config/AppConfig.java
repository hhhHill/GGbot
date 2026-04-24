package org.example.ggbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.feishu.FeishuProperties;
import org.example.ggbot.llm.LlmProperties;
import org.example.ggbot.tool.Tool;
import org.example.ggbot.tool.ToolRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Data
@RequiredArgsConstructor
@EnableConfigurationProperties({FeishuProperties.class, LlmProperties.class})
public class AppConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean
    public ToolRegistry toolRegistry(List<Tool> tools) {
        return ToolRegistry.from(tools);
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
