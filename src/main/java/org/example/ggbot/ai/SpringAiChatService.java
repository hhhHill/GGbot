package org.example.ggbot.ai;

import java.util.Optional;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * 基于 Spring AI {@link ChatClient} 的轻量模型调用入口。
 *
 * <p>当前阶段只负责统一项目内的聊天模型访问方式，
 * 不直接参与 Agent 主循环的规划、执行和反思逻辑。
 */
@Service
@Data
@RequiredArgsConstructor
public class SpringAiChatService {

    private final Optional<ChatClient> chatClient;

    public boolean isAvailable() {
        return chatClient.isPresent();
    }

    public String chat(String prompt) {
        return chat(null, prompt);
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (chatClient.isEmpty()) {
            return "Spring AI chat model is not configured. Prompt: " + userPrompt;
        }

        ChatClient.ChatClientRequestSpec requestSpec = chatClient.get().prompt();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            requestSpec = requestSpec.system(systemPrompt);
        }
        return requestSpec.user(userPrompt).call().content();
    }
}
