package org.example.ggbot.llm;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private boolean enabled;
    private String baseUrl;
    private String apiKey;
    private String model;
}
