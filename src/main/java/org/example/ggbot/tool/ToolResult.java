package org.example.ggbot.tool;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ToolResult {

    private final ToolName toolName;
    private final boolean success;
    private final String summary;
    private final Object artifact;
}
