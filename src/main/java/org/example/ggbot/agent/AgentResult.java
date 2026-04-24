package org.example.ggbot.agent;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.planner.IntentType;

@Data
@RequiredArgsConstructor
public class AgentResult {

    private final String taskId;
    private final IntentType intentType;
    private final String replyText;
    private final List<String> artifactSummaries;
}
