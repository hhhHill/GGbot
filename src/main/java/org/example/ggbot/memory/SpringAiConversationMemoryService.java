package org.example.ggbot.memory;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

/**
 * 基于 Spring AI {@link ChatMemory} 的会话记忆实现。
 */
@Service
@Data
@RequiredArgsConstructor
public class SpringAiConversationMemoryService implements ConversationMemoryService {

    private final ChatMemory chatMemory;

    @Override
    public List<String> getConversationHistory(String conversationId) {
        return getMessages(conversationId).stream()
                .map(this::toHistoryLine)
                .toList();
    }

    @Override
    public List<Message> getMessages(String conversationId) {
        return chatMemory.get(conversationId);
    }

    @Override
    public void appendUserMessage(String conversationId, String userMessage) {
        chatMemory.add(conversationId, new UserMessage(userMessage));
    }

    @Override
    public void appendAgentMessage(String conversationId, String agentMessage) {
        chatMemory.add(conversationId, new AssistantMessage(agentMessage));
    }

    private String toHistoryLine(Message message) {
        String prefix = switch (message.getMessageType()) {
            case USER -> "USER: ";
            case ASSISTANT -> "AGENT: ";
            default -> message.getMessageType().name() + ": ";
        };
        String text;
        if (message instanceof UserMessage userMessage) {
            text = userMessage.getText();
        } else if (message instanceof AssistantMessage assistantMessage) {
            text = assistantMessage.getText();
        } else if (message instanceof AbstractMessage abstractMessage) {
            text = abstractMessage.getText();
        } else {
            text = message.toString();
        }
        return prefix + text;
    }
}
