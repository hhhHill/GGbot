package org.example.ggbot.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Data
@RequiredArgsConstructor
public class InMemoryConversationMemoryService implements ConversationMemoryService {

    private final ConcurrentMap<String, List<String>> memoryStore = new ConcurrentHashMap<>();

    @Override
    public List<String> getConversationHistory(String conversationId) {
        return new ArrayList<>(memoryStore.getOrDefault(conversationId, new ArrayList<>()));
    }

    @Override
    public void appendUserMessage(String conversationId, String userMessage) {
        memoryStore.computeIfAbsent(conversationId, key -> new ArrayList<>()).add("USER: " + userMessage);
    }

    @Override
    public void appendAgentMessage(String conversationId, String agentMessage) {
        memoryStore.computeIfAbsent(conversationId, key -> new ArrayList<>()).add("AGENT: " + agentMessage);
    }
}
