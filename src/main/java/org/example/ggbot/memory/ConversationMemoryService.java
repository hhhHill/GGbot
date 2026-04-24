package org.example.ggbot.memory;

import java.util.List;

public interface ConversationMemoryService {

    List<String> getConversationHistory(String conversationId);

    void appendUserMessage(String conversationId, String userMessage);

    void appendAgentMessage(String conversationId, String agentMessage);
}
