package org.example.ggbot.tool;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ToolRegistry {

    private final Map<ToolName, Tool> tools;

    public static ToolRegistry from(List<Tool> toolList) {
        return new ToolRegistry(toolList.stream().collect(Collectors.toMap(Tool::toolName, Function.identity())));
    }

    public Tool getTool(ToolName toolName) {
        Tool tool = tools.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unsupported tool: " + toolName);
        }
        return tool;
    }
}
