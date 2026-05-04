package org.example.ggbot.adapter.feishu;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.example.ggbot.adapter.feishu.dto.FeishuWebhookRequest;
import org.example.ggbot.common.JsonUtils;
import org.junit.jupiter.api.Test;

class FeishuEventParserTest {

    @Test
    void shouldParseInboundMessageForTenantScopedHandling() {
        FeishuEventParser parser = new FeishuEventParser(new JsonUtils(new ObjectMapper()));
        FeishuWebhookRequest request = new FeishuWebhookRequest();
        request.setHeader(Map.of("event_type", "im.message.receive_v1"));
        request.setEvent(Map.of(
                "tenant_key", "tenant-1",
                "message", Map.of(
                        "chat_id", "oc_group_1",
                        "chat_type", "group",
                        "message_id", "om_1",
                        "content", "{\"text\":\"@bot hello\"}"
                ),
                "chat", Map.of("name", "Agent Group"),
                "sender", Map.of(
                        "sender_id", Map.of("open_id", "ou_123"),
                        "sender_type", "user"
                ),
                "sender_name", "Alice",
                "sender_avatar", Map.of("avatar_origin", "https://avatar.example/alice.png")
        ));

        FeishuInboundMessage message = parser.toInboundMessage(request);

        assertThat(message.tenantKey()).isEqualTo("tenant-1");
        assertThat(message.openId()).isEqualTo("ou_123");
        assertThat(message.chatId()).isEqualTo("oc_group_1");
        assertThat(message.chatType()).isEqualTo("group");
        assertThat(message.messageId()).isEqualTo("om_1");
        assertThat(message.messageContent()).isEqualTo("@bot hello");
        assertThat(message.chatName()).isEqualTo("Agent Group");
        assertThat(message.senderNickname()).isEqualTo("Alice");
        assertThat(message.senderAvatar()).isEqualTo("https://avatar.example/alice.png");
    }
}
