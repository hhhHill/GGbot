package org.example.ggbot.adapter.feishu;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.feishu.dto.FeishuWebhookRequest;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.common.JsonUtils;
import org.springframework.stereotype.Component;

@Component
@Data
@RequiredArgsConstructor
public class FeishuEventParser {

    private final JsonUtils jsonUtils;

    @SuppressWarnings("unchecked")
    public AgentRequest toAgentRequest(FeishuWebhookRequest request) {
        Map<String, Object> event = request.getEvent() == null ? Map.of() : request.getEvent();
        Map<String, Object> message = safeMap(event.get("message"));
        Map<String, Object> sender = safeMap(event.get("sender"));
        Map<String, Object> senderId = safeMap(sender.get("sender_id"));

        String rawContent = asString(message.get("content"));
        Map<String, Object> contentMap = jsonUtils.toMap(rawContent);
        String text = asString(contentMap.getOrDefault("text", rawContent));
        String chatId = asString(message.get("chat_id"));
        String messageId = asString(message.get("message_id"));
        String userId = asString(senderId.get("user_id"));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("chatId", chatId);
        metadata.put("messageId", messageId);
        metadata.put("senderId", userId);

        return new AgentRequest(
                chatId,
                userId,
                text,
                AgentChannel.FEISHU,
                messageId,
                chatId,
                metadata
        );
    }

    public String eventType(FeishuWebhookRequest request) {
        return asString(safeMap(request.getHeader()).get("event_type"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }
}
