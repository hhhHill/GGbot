package org.example.ggbot.ai;

/**
 * 统一定义可靠聊天调用的重试与兜底策略。
 */
public final class ChatFallbackPolicy {

    private static final int DEFAULT_MAX_ATTEMPTS = 4;
    private static final String DEFAULT_FALLBACK_REPLY = "抱歉，我暂时无法处理你的请求，请稍后再试。";

    private final int maxAttempts;
    private final String fallbackReply;

    private ChatFallbackPolicy(int maxAttempts, String fallbackReply) {
        this.maxAttempts = maxAttempts;
        this.fallbackReply = fallbackReply;
    }

    public static ChatFallbackPolicy createDefault() {
        return new ChatFallbackPolicy(DEFAULT_MAX_ATTEMPTS, DEFAULT_FALLBACK_REPLY);
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public String fallbackReply() {
        return fallbackReply;
    }
}
