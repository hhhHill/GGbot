package org.example.ggbot.adapter.feishu;

public record FeishuInboundMessage(
        String tenantKey,
        String tenantName,
        String openId,
        String chatId,
        String chatName,
        String chatType,
        String messageId,
        String messageContent,
        String senderNickname,
        String senderAvatar
) {
}
