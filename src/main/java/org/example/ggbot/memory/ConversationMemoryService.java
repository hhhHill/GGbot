package org.example.ggbot.memory;

import java.util.List;
import org.springframework.ai.chat.messages.Message;

public interface ConversationMemoryService {

    List<String> getConversationHistory(String conversationId);

    List<Message> getMessages(String conversationId);

    void appendUserMessage(String conversationId, String userMessage);

    void appendAgentMessage(String conversationId, String agentMessage);
}
