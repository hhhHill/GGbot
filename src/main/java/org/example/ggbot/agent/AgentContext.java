package org.example.ggbot.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class AgentContext {

    private final String taskId;
    private final String conversationId;
    private final String userId;
    private final AgentChannel channel;
    private final List<String> conversationHistory;
    private final Map<String, Object> metadata;

    public AgentContext(
            String taskId,
            String conversationId,
            String userId,
            AgentChannel channel,
            List<String> conversationHistory,
            Map<String, Object> metadata) {
        this.taskId = taskId;
        this.conversationId = conversationId;
        this.userId = userId;
        this.channel = channel;
        this.conversationHistory = conversationHistory == null ? new ArrayList<>() : new ArrayList<>(conversationHistory);
        this.metadata = metadata == null ? Map.of() : metadata;
    }

    public void addConversationMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        conversationHistory.add(message);
    }
}
