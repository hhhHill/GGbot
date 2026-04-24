package org.example.ggbot.tool;

public interface Tool {

    ToolName toolName();

    ToolResult execute(ToolRequest request);
}
