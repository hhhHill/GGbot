package org.example.ggbot.adapter.web.dto;

import lombok.Data;

@Data
public class ChatSendRequest {

    private String sessionId;
    private String userId;
    private String message;
}
