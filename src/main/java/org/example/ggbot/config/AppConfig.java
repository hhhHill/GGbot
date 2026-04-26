package org.example.ggbot.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.feishu.FeishuProperties;
import org.example.ggbot.tool.impl.GenerateDocTool;
import org.example.ggbot.tool.impl.GeneratePptTool;
import org.example.ggbot.tool.impl.ModifyPptTool;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@Data
@RequiredArgsConstructor
@EnableConfigurationProperties(FeishuProperties.class)
public class AppConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(
            GenerateDocTool generateDocTool,
            GeneratePptTool generatePptTool,
            ModifyPptTool modifyPptTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(generateDocTool, generatePptTool, modifyPptTool)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
