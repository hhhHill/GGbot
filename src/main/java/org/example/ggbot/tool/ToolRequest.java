package org.example.ggbot.tool;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentContext;

@Data
@RequiredArgsConstructor
public class ToolRequest {

    private final String taskId;
    private final String prompt;
    private final AgentContext context;
    private final Map<String, Object> parameters;
}
