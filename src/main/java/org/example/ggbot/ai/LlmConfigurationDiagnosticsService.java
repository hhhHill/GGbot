package org.example.ggbot.ai;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * 启动时输出当前 LLM 配置解析结果的诊断服务。
 */
@Service
@Slf4j
@Data
@RequiredArgsConstructor
public class LlmConfigurationDiagnosticsService {

    private final Environment environment;
    private final Optional<ChatModel> chatModel;
    private final Optional<ChatClient> chatClient;
    private final Optional<ChatClient.Builder> chatClientBuilder;

    public void logResolvedConfiguration() {
        Map<String, String> values = resolvedConfiguration();
        log.info("LLM configuration diagnostics: {}", values);
        log.info("LLM bean diagnostics: {}", resolvedBeanDiagnostics());
    }

    public Map<String, String> resolvedConfiguration() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("spring.ai.openai.base-url", property("spring.ai.openai.base-url"));
        values.put("spring.ai.openai.chat.options.model", property("spring.ai.openai.chat.options.model"));
        values.put("spring.ai.openai.api-key", mask(property("spring.ai.openai.api-key")));
        values.put("SPRING_AI_OPENAI_BASE_URL", property("SPRING_AI_OPENAI_BASE_URL"));
        values.put("SPRING_AI_OPENAI_MODEL", property("SPRING_AI_OPENAI_MODEL"));
        values.put("SPRING_AI_OPENAI_API_KEY", mask(property("SPRING_AI_OPENAI_API_KEY")));
        return values;
    }

    public Map<String, String> resolvedBeanDiagnostics() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("ChatModel", beanState(chatModel));
        values.put("ChatClient", beanState(chatClient));
        values.put("ChatClient.Builder", beanState(chatClientBuilder));
        return values;
    }

    private String property(String key) {
        return environment.getProperty(key, "<null>");
    }

    private String beanState(Optional<?> bean) {
        if (bean.isEmpty()) {
            return "<missing>";
        }
        return "<present> " + bean.get().getClass().getName();
    }

    String mask(String value) {
        if (value == null || value.isBlank() || "<null>".equals(value)) {
            return "<null>";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
