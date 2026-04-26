package org.example.ggbot.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 为 Spring AI 聊天调用补充通用重试与兜底。
 */
@Service
public class ReliableChatService {

    private static final Logger log = LoggerFactory.getLogger(ReliableChatService.class);

    private final SpringAiChatService delegate;
    private final ChatFallbackPolicy fallbackPolicy;

    @Autowired
    public ReliableChatService(SpringAiChatService delegate) {
        this(delegate, ChatFallbackPolicy.createDefault());
    }

    ReliableChatService(SpringAiChatService delegate, ChatFallbackPolicy fallbackPolicy) {
        this.delegate = delegate;
        this.fallbackPolicy = fallbackPolicy;
    }

    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    public String chat(String prompt) {
        return chat(null, prompt);
    }

    public String chat(String systemPrompt, String userPrompt) {
        RuntimeException lastException = null;
        int maxAttempts = fallbackPolicy.maxAttempts();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String reply = delegate.chat(systemPrompt, userPrompt);
                if (reply != null && !reply.isBlank()) {
                    return reply;
                }
                log.warn("Chat attempt {}/{} returned blank content; retrying", attempt, maxAttempts);
            } catch (RuntimeException ex) {
                lastException = ex;
                log.warn(
                        "Chat attempt {}/{} failed with {}",
                        attempt,
                        maxAttempts,
                        ex.getClass().getSimpleName(),
                        ex
                );
            }
        }
        if (lastException != null) {
            log.error("All chat attempts failed; returning fallback reply", lastException);
        } else {
            log.error("All chat attempts returned blank content; returning fallback reply");
        }
        return fallbackPolicy.fallbackReply();
    }
}
