package org.example.ggbot.adapter.web.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class WebChatRequest {

    private String conversationId;
    private String userId;
    private String message;
}
