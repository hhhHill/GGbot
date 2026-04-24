package org.example.ggbot.adapter.feishu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FeishuWebhookRequest {

    private String challenge;
    private Map<String, Object> header;
    private Map<String, Object> event;
}
