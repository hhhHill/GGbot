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
        Map<String, Object> chat = safeMap(event.get("chat"));
        Map<String, Object> senderAvatar = safeMap(event.get("sender_avatar"));

        String rawContent = asString(message.get("content"));
        Map<String, Object> contentMap = jsonUtils.toMap(rawContent);
        String text = asString(contentMap.getOrDefault("text", rawContent));
        String chatId = asString(message.get("chat_id"));
        String messageId = asString(message.get("message_id"));
        String openId = firstNonBlank(asString(senderId.get("open_id")), asString(senderId.get("user_id")));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tenantKey", firstNonBlank(
                asString(event.get("tenant_key")),
                asString(sender.get("tenant_key")),
                asString(safeMap(request.getHeader()).get("tenant_key"))
        ));
        metadata.put("tenantName", firstNonBlank(asString(chat.get("tenant_name")), asString(event.get("tenant_name"))));
        metadata.put("chatId", chatId);
        metadata.put("chatName", firstNonBlank(asString(chat.get("name")), chatId));
        metadata.put("chatType", asString(message.get("chat_type")));
        metadata.put("messageId", messageId);
        metadata.put("senderId", openId);
        metadata.put("openId", openId);
        metadata.put("senderNickname", firstNonBlank(asString(event.get("sender_name")), asString(sender.get("name"))));
        metadata.put("senderAvatar", firstNonBlank(asString(senderAvatar.get("avatar_origin")), asString(sender.get("avatar_url"))));

        return new AgentRequest(
                chatId,
                openId,
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

    public FeishuInboundMessage toInboundMessage(FeishuWebhookRequest request) {
        Map<String, Object> event = request.getEvent() == null ? Map.of() : request.getEvent();
        Map<String, Object> message = safeMap(event.get("message"));
        Map<String, Object> sender = safeMap(event.get("sender"));
        Map<String, Object> senderId = safeMap(sender.get("sender_id"));
        Map<String, Object> chat = safeMap(event.get("chat"));
        Map<String, Object> senderAvatar = safeMap(event.get("sender_avatar"));

        String rawContent = asString(message.get("content"));
        Map<String, Object> contentMap = jsonUtils.toMap(rawContent);
        String text = asString(contentMap.getOrDefault("text", rawContent));

        return new FeishuInboundMessage(
                firstNonBlank(
                        asString(event.get("tenant_key")),
                        asString(sender.get("tenant_key")),
                        asString(safeMap(request.getHeader()).get("tenant_key"))
                ),
                firstNonBlank(asString(chat.get("tenant_name")), asString(event.get("tenant_name"))),
                firstNonBlank(asString(senderId.get("open_id")), asString(senderId.get("user_id"))),
                asString(message.get("chat_id")),
                firstNonBlank(asString(chat.get("name")), asString(message.get("chat_id"))),
                asString(message.get("chat_type")),
                asString(message.get("message_id")),
                text,
                firstNonBlank(asString(event.get("sender_name")), asString(sender.get("name"))),
                firstNonBlank(asString(senderAvatar.get("avatar_origin")), asString(sender.get("avatar_url")))
        );
    }

    public String externalEventId(FeishuWebhookRequest request) {
        Map<String, Object> event = request.getEvent() == null ? Map.of() : request.getEvent();
        Map<String, Object> message = safeMap(event.get("message"));
        String messageId = asString(message.get("message_id"));
        if (!messageId.isBlank()) {
            return messageId;
        }
        return asString(safeMap(request.getHeader()).get("event_id"));
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
