package org.example.ggbot.adapter.web.dto;

import lombok.Data;

@Data
public class WebChatSendRequest {

    private String sessionId;
    private Long orgId;
    private Long conversationId;
    private String messageContent;
}
