package org.example.ggbot.tool.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.model.Slide;
import org.example.ggbot.tool.ppt.PptSpec;
import org.example.ggbot.tool.ppt.PptxRenderer;
import org.example.ggbot.tool.ppt.SemanticPptFallbackGenerator;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Data
public class ModifyPptTool {

    private static final String MODIFY_INSTRUCTION_NAME = "ppt-modify-instruction.txt";

    private final SemanticPptFallbackGenerator semanticPptFallbackGenerator;
    private final PptxRenderer renderer;
    private final ClasspathPromptRepository promptRepository;

    public ModifyPptTool(
            SemanticPptFallbackGenerator semanticPptFallbackGenerator,
            PptxRenderer renderer,
            ClasspathPromptRepository promptRepository) {
        this.semanticPptFallbackGenerator = semanticPptFallbackGenerator;
        this.renderer = renderer;
        this.promptRepository = promptRepository;
    }

    @Tool(name = "modifyPpt", description = "根据补充要求调整现有 PPT")
    public PptArtifact modifyPpt(@ToolParam(description = "用户给出的 PPT 修改要求") String prompt) {
        ToolResult result = execute(prompt, null, Map.of());
        return (PptArtifact) result.getArtifact();
    }

    public ToolResult execute(String prompt, AgentContext context, Map<String, Object> parameters) {
        String instruction = promptRepository.load(MODIFY_INSTRUCTION_NAME, Map.of("prompt", prompt == null ? "" : prompt.trim()));
        PptSpec spec = semanticPptFallbackGenerator.generate(instruction);
        Path output = renderer.render(spec);
        PptArtifact artifact = new PptArtifact(
                spec.getTitle(),
                spec.getSlides().stream().map(slide -> new Slide(
                        slide.getPageNumber(),
                        slide.getTitle(),
                        slide.getSubtitle(),
                        slide.getBullets(),
                        slide.getSpeakerNotes())).toList(),
                output.toString()
        );
        return new ToolResult(toolName(), true, "已根据修改要求重新生成 PPTX 文件：" + output, artifact);
    }

    private ToolName toolName() {
        return ToolName.MODIFY_PPT;
    }
}
