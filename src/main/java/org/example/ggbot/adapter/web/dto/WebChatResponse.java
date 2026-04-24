package org.example.ggbot.adapter.web.dto;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class WebChatResponse {

    private final String taskId;
    private final String replyText;
    private final String intentType;
    private final List<String> artifactSummaries;
}
