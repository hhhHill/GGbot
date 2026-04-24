package org.example.ggbot.llm;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Data
@RequiredArgsConstructor
public class OpenAiCompatibleLlmClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties llmProperties;

    @Override
    public String chat(String prompt, Map<String, Object> options) {
        if (!llmProperties.isEnabled() || llmProperties.getApiKey() == null || llmProperties.getApiKey().isBlank()) {
            return "LLM is disabled. Prompt: " + prompt;
        }

        Map<String, Object> body = Map.of(
                "model", llmProperties.getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        Map<?, ?> response = restClient.post()
                .uri(llmProperties.getBaseUrl() + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + llmProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

        Object choicesObject = response == null ? null : response.get("choices");
        if (choicesObject instanceof List<?> choices && !choices.isEmpty()) {
            Object first = choices.get(0);
            if (first instanceof Map<?, ?> firstChoice) {
                Object message = firstChoice.get("message");
                if (message instanceof Map<?, ?> messageMap) {
                    Object content = messageMap.get("content");
                    if (content != null) {
                        return content.toString();
                    }
                }
            }
        }
        return "LLM returned no content.";
    }
}
