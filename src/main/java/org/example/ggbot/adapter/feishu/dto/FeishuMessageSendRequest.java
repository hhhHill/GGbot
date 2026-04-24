package org.example.ggbot.adapter.feishu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class FeishuMessageSendRequest {

    @JsonProperty("receive_id")
    private final String receiveId;

    @JsonProperty("msg_type")
    private final String msgType;

    private final String content;
}
