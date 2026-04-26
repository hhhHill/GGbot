package org.example.ggbot.ai;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 应用启动后的大模型连通性探测服务。
 */
@Service
@Slf4j
@Data
@RequiredArgsConstructor
public class LlmStartupProbeService {

    private static final String PROBE_SYSTEM_PROMPT = "Reply with OK only.";
    private static final String PROBE_USER_PROMPT = "OK";

    private final SpringAiChatService chatService;
    private final AtomicBoolean llmConfigured = new AtomicBoolean(false);
    private final AtomicBoolean llmReachable = new AtomicBoolean(false);
    private final AtomicReference<String> llmMessage = new AtomicReference<>("启动后尚未完成模型探测");

    public void runStartupProbe() {
        if (!chatService.isAvailable()) {
            llmConfigured.set(false);
            llmReachable.set(false);
            llmMessage.set("模型未配置，已跳过启动探测");
            log.warn("LLM startup probe skipped: model is not configured");
            return;
        }

        llmConfigured.set(true);
        llmMessage.set("正在探测模型连通性");

        try {
            String reply = chatService.chat(PROBE_SYSTEM_PROMPT, PROBE_USER_PROMPT);
            llmReachable.set(reply != null && !reply.isBlank());
            if (llmReachable.get()) {
                llmMessage.set("模型启动探测成功");
                log.info("LLM startup probe succeeded with reply: {}", reply);
            } else {
                llmMessage.set("模型启动探测失败：返回空响应");
                log.warn("LLM startup probe failed: empty response");
            }
        } catch (RuntimeException ex) {
            llmReachable.set(false);
            llmMessage.set("模型启动探测失败：" + ex.getMessage());
            log.warn("LLM startup probe failed", ex);
        }
    }

    public boolean isLlmConfigured() {
        return llmConfigured.get();
    }

    public boolean isLlmReachable() {
        return llmReachable.get();
    }

    public String getLlmMessage() {
        return llmMessage.get();
    }
}
