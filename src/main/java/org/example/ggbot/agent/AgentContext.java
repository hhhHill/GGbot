package org.example.ggbot.agent;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AgentContext {

    private final String taskId;
    private final String conversationId;
    private final String userId;
    private final AgentChannel channel;
    private final List<String> conversationHistory;
    private final Map<String, Object> metadata;
}
