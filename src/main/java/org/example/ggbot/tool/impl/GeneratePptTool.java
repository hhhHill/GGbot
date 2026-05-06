package org.example.ggbot.tool.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.model.Slide;
import org.example.ggbot.tool.ppt.PptSpec;
import org.example.ggbot.tool.ppt.PptSpecGenerationClient;
import org.example.ggbot.tool.ppt.PptSpecParser;
import org.example.ggbot.tool.ppt.PptSpecValidator;
import org.example.ggbot.tool.ppt.PptxRenderer;
import org.example.ggbot.tool.ppt.SemanticPptFallbackGenerator;
import org.example.ggbot.tool.support.ArtifactContentExtractor;
import org.example.ggbot.tool.support.PromptDetailAnalyzer;
import org.example.ggbot.tool.support.PromptDetailLevel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Data
public class GeneratePptTool {

    private static final String STEP_RESULTS_SECTION_NAME = "ppt-step-results-section.txt";
    private static final String RETRY_INSTRUCTION_NAME = "ppt-retry-fix-instruction.txt";
    private static final String USER_PROMPT_NAME = "generate-ppt-user-prompt.txt";

    private final PptSpecGenerationClient generationClient;
    private final PptSpecParser parser;
    private final PptSpecValidator validator;
    private final SemanticPptFallbackGenerator fallbackGenerator;
    private final PptxRenderer renderer;
    private final ArtifactContentExtractor artifactContentExtractor;
    private final ClasspathPromptRepository promptRepository;
    private final PromptDetailAnalyzer promptDetailAnalyzer;

    public GeneratePptTool(
            PptSpecGenerationClient generationClient,
            PptSpecParser parser,
            PptSpecValidator validator,
            SemanticPptFallbackGenerator fallbackGenerator,
            PptxRenderer renderer,
            ArtifactContentExtractor artifactContentExtractor,
            ClasspathPromptRepository promptRepository,
            PromptDetailAnalyzer promptDetailAnalyzer) {
        this.generationClient = generationClient;
        this.parser = parser;
        this.validator = validator;
        this.fallbackGenerator = fallbackGenerator;
        this.renderer = renderer;
        this.artifactContentExtractor = artifactContentExtractor;
        this.promptRepository = promptRepository;
        this.promptDetailAnalyzer = promptDetailAnalyzer;
    }

    @Tool(name = "generatePpt", description = "根据用户需求生成真实 PPTX 文件")
    public PptArtifact generatePpt(@ToolParam(description = "用户当前的 PPT 生成需求") String prompt) {
        ToolResult result = execute(prompt, null, Map.of());
        return (PptArtifact) result.getArtifact();
    }

    public ToolResult execute(String prompt, AgentContext context, Map<String, Object> parameters) {
        log.info("=== 开始PPT生成流程 ===");
        log.info("用户输入prompt: {}", prompt);
        
        String rawInput = buildRawInput(prompt, parameters);
        log.info("已构建生成用输入文本，长度: {}字符", rawInput.length());
        
        PromptDetailLevel detailLevel = java.util.Optional.ofNullable(promptDetailAnalyzer.analyze(rawInput))
                .orElse(PromptDetailLevel.NORMAL);
        log.info("已分析prompt详细程度: {}", detailLevel.name());
        
        String instruction = promptRepository.load(USER_PROMPT_NAME, Map.of(
                "prompt", rawInput,
                "promptDetailLevel", detailLevel.name()
        ));
        log.info("已加载生成提示词模板");
        
        PptSpec spec = generateWithRetry(instruction, rawInput, detailLevel, rawInput.length());
        PptSpecValidator.ValidationResult validation = validator.validate(spec, rawInput, detailLevel, rawInput.length());
        if (!validation.valid()) {
            log.error("PPT内容校验失败: {}", validation.errors());
            return new ToolResult(toolName(), false, "PPT 生成失败，无法得到有效内容。", null);
        }
        log.info("PPT内容校验通过，共{}页", spec.getSlides().size());
        
        log.info("开始渲染PPTX文件");
        Path output = renderer.render(spec);
        log.info("PPTX文件渲染完成，输出路径: {}", output);
        
        PptArtifact artifact = toArtifact(spec, output);
        log.info("=== PPT生成流程结束，成功完成 ===");
        
        return new ToolResult(toolName(), true, "已生成 PPTX 文件：" + output, artifact);
    }

    private ToolName toolName() {
        return ToolName.GENERATE_PPT;
    }

    private String buildRawInput(String prompt, Map<String, Object> parameters) {
        StringBuilder builder = new StringBuilder();
        builder.append(prompt == null ? "" : prompt.trim());
        Object stepResults = parameters == null ? null : parameters.get("stepResults");
        if (stepResults instanceof Map<?, ?> resultMap && !resultMap.isEmpty()) {
            String readable = resultMap.entrySet().stream()
                    .map(entry -> {
                        Object value = entry.getValue();
                        if (value instanceof ToolResult toolResult) {
                            return entry.getKey() + ":\n" + artifactContentExtractor.extract(toolResult);
                        }
                        return entry.getKey() + ":\n" + String.valueOf(value);
                    })
                    .collect(Collectors.joining("\n\n"));
            builder.append("\n\n").append(promptRepository.load(STEP_RESULTS_SECTION_NAME, Map.of("stepResults", readable)));
        }
        return builder.toString().trim();
    }

    private PptSpec generateWithRetry(
            String instruction,
            String validationInput,
            PromptDetailLevel detailLevel,
            int originalInputLength) {
        List<String> issues = List.of();
        for (int attempt = 0; attempt < 2; attempt++) {
            log.info("执行PPT内容生成尝试 {}/2", attempt + 1);
            try {
                String raw = generationClient.generate(buildRetryInstruction(instruction, issues));
                log.info("已获取LLM返回内容，长度: {}字符", raw.length());
                
                PptSpec spec = parser.parse(raw);
                log.info("JSON解析成功");
                
                PptSpecValidator.ValidationResult validation = validator.validate(spec, validationInput, detailLevel, originalInputLength);
                if (validation.valid()) {
                    log.info("尝试{}校验通过", attempt + 1);
                    return spec;
                }
                issues = validation.errors();
                log.warn("尝试{}校验失败: {}", attempt + 1, validation.errors());
            } catch (RuntimeException ex) {
                issues = List.of("PptSpec JSON parse failed");
                log.warn("尝试{}解析失败: {}", attempt + 1, ex.getMessage());
            }
        }
        log.info("所有尝试失败，启用兜底生成器");
        return fallbackGenerator.generate(instruction);
    }

    private String buildRetryInstruction(String instruction, List<String> issues) {
        if (issues == null || issues.isEmpty()) {
            return instruction;
        }
        return instruction + "\n\n" + promptRepository.load(RETRY_INSTRUCTION_NAME, Map.of(
                "issues", "- " + String.join("\n- ", issues)
        ));
    }

    private PptArtifact toArtifact(PptSpec spec, Path output) {
        List<Slide> slides = spec.getSlides().stream()
                .map(slide -> new Slide(
                        slide.getPageNumber(),
                        slide.getTitle(),
                        slide.getSubtitle(),
                        slide.getBullets(),
                        slide.getSpeakerNotes()))
                .toList();
        return new PptArtifact(spec.getTitle(), slides, output.toString());
    }
}
