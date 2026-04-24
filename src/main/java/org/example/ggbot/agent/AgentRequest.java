package org.example.ggbot.agent;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AgentRequest {

    private final String conversationId;
    private final String userId;
    private final String userInput;
    private final AgentChannel channel;
    private final String channelMessageId;
    private final String replyTargetId;
    private final Map<String, Object> metadata;
}
