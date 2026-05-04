package org.example.ggbot.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.example.ggbot.agent.AgentContext;
import org.springframework.stereotype.Component;

@Component
public class MemoryManager {

    static final int RECENT_ROUNDS = 5;
    private static final int RECENT_MESSAGES_LIMIT = RECENT_ROUNDS * 2;
    private static final String USER_PREFIX = "USER: ";
    private static final String ASSISTANT_PREFIX = "ASSISTANT: ";

    public String buildPrompt(String currentInput, AgentContext context) {
        String normalizedInput = normalizeContent(currentInput);
        List<String> recentMessages = getRecentMessages(context == null ? List.of() : context.getConversationHistory());
        if (recentMessages.isEmpty()) {
            return """
                    === 当前问题 ===
                    USER: %s""".formatted(normalizedInput);
        }
        return """
                === 最近对话 ===
                %s
                
                === 当前问题 ===
                USER: %s""".formatted(formatRecentMessages(recentMessages), normalizedInput);
    }

    public void addUserMessage(AgentContext context, String content) {
        addMessage(context, USER_PREFIX, content);
    }

    public void addAssistantMessage(AgentContext context, String content) {
        addMessage(context, ASSISTANT_PREFIX, content);
    }

    public List<String> getRecentMessages(List<String> history) {
        List<String> validMessages = new ArrayList<>();
        if (history != null) {
            for (String entry : history) {
                if (isDialogueMessage(entry)) {
                    validMessages.add(entry);
                }
            }
        }
        int fromIndex = Math.max(0, validMessages.size() - RECENT_MESSAGES_LIMIT);
        return new ArrayList<>(validMessages.subList(fromIndex, validMessages.size()));
    }

    public String formatRecentMessages(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return String.join("\n", messages);
    }

    private void addMessage(AgentContext context, String prefix, String content) {
        if (context == null) {
            return;
        }
        String normalizedContent = normalizeContent(content);
        if (normalizedContent.isBlank()) {
            return;
        }
        String message = prefix + normalizedContent;
        List<String> history = context.getConversationHistory();
        if (!history.isEmpty() && Objects.equals(history.get(history.size() - 1), message)) {
            return;
        }
        context.addConversationMessage(message);
    }

    private boolean isDialogueMessage(String entry) {
        return entry != null && (entry.startsWith(USER_PREFIX) || entry.startsWith(ASSISTANT_PREFIX));
    }

    private String normalizeContent(String content) {
        return content == null ? "" : content.trim();
    }
}
