# PPT Spec Rendering Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the demo PPT template flow with `PptSpec` JSON generation plus Apache POI `.pptx` rendering, while preventing internal object strings from leaking into user-visible content.

**Architecture:** Keep planning and graph orchestration unchanged. Concentrate the change in the tool chain by introducing a readable artifact extractor, a `PptSpec` generation/validation/rendering pipeline, and minimal semantic fallbacks for doc and PPT tools. Use Spring AI for JSON generation when available and Apache POI for file rendering.

**Tech Stack:** Java 21, Spring Boot, Spring AI, Jackson, Apache POI OOXML, JUnit 5, AssertJ, Mockito

---

## File Structure

- Modify: `pom.xml`
  Responsibility: add Apache POI OOXML dependency.
- Create: `src/main/java/org/example/ggbot/tool/ppt/PptSpec.java`
  Responsibility: top-level PPT spec model.
- Create: `src/main/java/org/example/ggbot/tool/ppt/SlideSpec.java`
  Responsibility: per-slide spec model.
- Create: `src/main/java/org/example/ggbot/tool/ppt/PptSpecParser.java`
  Responsibility: sanitize and parse strict JSON into `PptSpec`.
- Create: `src/main/java/org/example/ggbot/tool/ppt/PptSpecValidator.java`
  Responsibility: validate `PptSpec` and return structured validation result.
- Create: `src/main/java/org/example/ggbot/tool/ppt/PptSpecGenerationClient.java`
  Responsibility: abstraction for LLM JSON generation.
- Create: `src/main/java/org/example/ggbot/tool/ppt/SpringAiPptSpecGenerationClient.java`
  Responsibility: Spring AI-backed implementation.
- Create: `src/main/java/org/example/ggbot/tool/ppt/SemanticPptFallbackGenerator.java`
  Responsibility: topic-based fallback PPT spec generation.
- Create: `src/main/java/org/example/ggbot/tool/ppt/PptxRenderer.java`
  Responsibility: render `PptSpec` into a `.pptx` file.
- Create: `src/main/java/org/example/ggbot/tool/support/ArtifactContentExtractor.java`
  Responsibility: convert tool results and artifacts into readable text.
- Create: `src/main/java/org/example/ggbot/tool/support/ResultQualityEvaluator.java`
  Responsibility: shared quality checks for internal-string leakage, template drift, and topic relevance.
- Modify: `src/main/java/org/example/ggbot/agent/execution/DefaultExecutor.java`
  Responsibility: materialize dependency inputs through readable content extraction.
- Modify: `src/main/java/org/example/ggbot/tool/model/PptArtifact.java`
  Responsibility: carry rendered file path in addition to title and slides.
- Modify: `src/main/java/org/example/ggbot/tool/impl/GeneratePptTool.java`
  Responsibility: replace hardcoded template with spec generation, validation, fallback, rendering, and artifact return.
- Modify: `src/main/java/org/example/ggbot/tool/impl/GenerateDocTool.java`
  Responsibility: minimal semantic document generation instead of fixed project-plan markdown.
- Modify: `src/main/java/org/example/ggbot/tool/impl/ModifyPptTool.java`
  Responsibility: regenerate PPT semantically instead of fixed modified template.
- Create: `src/test/java/org/example/ggbot/tool/ppt/PptSpecParserTest.java`
- Create: `src/test/java/org/example/ggbot/tool/ppt/PptSpecValidatorTest.java`
- Create: `src/test/java/org/example/ggbot/tool/ppt/PptxRendererTest.java`
- Create: `src/test/java/org/example/ggbot/tool/support/ArtifactContentExtractorTest.java`
- Create: `src/test/java/org/example/ggbot/tool/impl/GeneratePptToolTest.java`
- Modify: `src/test/java/org/example/ggbot/agent/execution/DefaultExecutorTest.java`
- Modify: `src/test/java/org/example/ggbot/tool/SpringAiToolExecutorTest.java`

### Task 1: Add Dependency and First Parser Test

**Files:**
- Modify: `pom.xml`
- Create: `src/test/java/org/example/ggbot/tool/ppt/PptSpecParserTest.java`

- [ ] **Step 1: Write the failing parser tests**

```java
package org.example.ggbot.tool.ppt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PptSpecParserTest {

    private final PptSpecParser parser = new PptSpecParser(new com.fasterxml.jackson.databind.ObjectMapper());

    @Test
    void shouldParseStandardJson() {
        String json = """
                {
                  "title": "红军长征",
                  "slides": [
                    {
                      "pageNumber": 1,
                      "title": "历史背景",
                      "bullets": ["第五次反围剿失利", "红军进行战略转移", "长征由此开始"]
                    }
                  ]
                }
                """;

        PptSpec spec = parser.parse(json);

        assertThat(spec.getTitle()).isEqualTo("红军长征");
        assertThat(spec.getSlides()).hasSize(1);
        assertThat(spec.getSlides().getFirst().getTitle()).isEqualTo("历史背景");
        assertThat(spec.getSlides().getFirst().getBullets()).hasSize(3);
    }

    @Test
    void shouldStripJsonCodeFence() {
        String json = """
                ```json
                {
                  "title": "红军长征",
                  "slides": [
                    {
                      "title": "遵义会议",
                      "bullets": ["确立新的军事领导", "成为战略转折点", "影响长征进程"]
                    }
                  ]
                }
                ```
                """;

        PptSpec spec = parser.parse(json);

        assertThat(spec.getSlides().getFirst().getPageNumber()).isEqualTo(1);
        assertThat(spec.getSlides().getFirst().getTitle()).isEqualTo("遵义会议");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=PptSpecParserTest test`
Expected: FAIL because `PptSpecParser`, `PptSpec`, and `SlideSpec` do not exist yet.

- [ ] **Step 3: Add Apache POI dependency and minimal models/parser**

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

```java
package org.example.ggbot.tool.ppt;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PptSpec {
    private String title;
    private List<SlideSpec> slides = new ArrayList<>();
}
```

```java
package org.example.ggbot.tool.ppt;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlideSpec {
    private int pageNumber;
    private String title;
    private List<String> bullets = new ArrayList<>();
    private String speakerNotes;
}
```

```java
package org.example.ggbot.tool.ppt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;

public class PptSpecParser {

    private final ObjectMapper objectMapper;

    public PptSpecParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public PptSpec parse(String rawContent) {
        try {
            PptSpec spec = objectMapper.readValue(stripCodeFence(rawContent), PptSpec.class);
            normalize(spec);
            return spec;
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to parse PPT spec JSON", ex);
        }
    }

    private String stripCodeFence(String rawContent) {
        String trimmed = rawContent == null ? "" : rawContent.trim();
        if (trimmed.startsWith("```")) {
            int firstLine = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLine >= 0 && lastFence > firstLine) {
                return trimmed.substring(firstLine + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private void normalize(PptSpec spec) {
        List<SlideSpec> slides = spec.getSlides() == null ? new ArrayList<>() : spec.getSlides();
        for (int index = 0; index < slides.size(); index++) {
            SlideSpec slide = slides.get(index);
            if (slide.getPageNumber() <= 0) {
                slide.setPageNumber(index + 1);
            }
            if (slide.getTitle() == null || slide.getTitle().isBlank()) {
                slide.setTitle("第 " + (index + 1) + " 页");
            }
            if (slide.getBullets() == null) {
                slide.setBullets(new ArrayList<>());
            }
        }
        spec.setSlides(slides);
    }
}
```

- [ ] **Step 4: Run parser tests to verify they pass**

Run: `mvn -Dtest=PptSpecParserTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java/org/example/ggbot/tool/ppt src/test/java/org/example/ggbot/tool/ppt/PptSpecParserTest.java
git commit -m "feat: add ppt spec parser foundation"
```

### Task 2: Add Validation and Quality Checks

**Files:**
- Create: `src/main/java/org/example/ggbot/tool/ppt/PptSpecValidator.java`
- Create: `src/main/java/org/example/ggbot/tool/support/ResultQualityEvaluator.java`
- Create: `src/test/java/org/example/ggbot/tool/ppt/PptSpecValidatorTest.java`

- [ ] **Step 1: Write the failing validation tests**

```java
package org.example.ggbot.tool.ppt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class PptSpecValidatorTest {

    private final PptSpecValidator validator = new PptSpecValidator();

    @Test
    void shouldAcceptValidHistoryTopicDeck() {
        PptSpec spec = new PptSpec("红军长征", List.of(
                new SlideSpec(1, "长征概览", List.of("重大历史事件", "战略转移过程", "汇报范围说明"), null),
                new SlideSpec(2, "历史背景", List.of("反围剿失利", "根据地压力", "战略转移启动"), null),
                new SlideSpec(3, "主要事件", List.of("突破封锁线", "四渡赤水", "会师北上"), null),
                new SlideSpec(4, "历史意义", List.of("保存革命力量", "锻造领导核心", "形成长征精神"), null)
        ));

        PptSpecValidator.ValidationResult result = validator.validate(spec, "请生成红军长征汇报 PPT");

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldRejectInternalObjectStringsAndDemoTemplate() {
        PptSpec spec = new PptSpec("汇报演示稿", List.of(
                new SlideSpec(1, "背景与目标", List.of("ToolResult(", "artifact=", "success="), null),
                new SlideSpec(2, "方案设计", List.of("模块拆分", "接口", "MVP"), null),
                new SlideSpec(3, "实施计划", List.of("里程碑", "资源与风险", "数据协作流程"), null),
                new SlideSpec(4, "结论与下一步", List.of("总结", "建议", "行动"), null)
        ));

        PptSpecValidator.ValidationResult result = validator.validate(spec, "请介绍红军长征");

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=PptSpecValidatorTest test`
Expected: FAIL because validator does not exist yet.

- [ ] **Step 3: Write minimal validator and quality evaluator**

```java
package org.example.ggbot.tool.support;

import java.util.ArrayList;
import java.util.List;

public class ResultQualityEvaluator {

    private static final List<String> INTERNAL_MARKERS = List.of(
            "ToolResult(",
            "DocumentArtifact(",
            "PptArtifact(",
            "markdown=",
            "artifact=",
            "toolName=",
            "success=",
            "org.example",
            "src/main/java"
    );

    public List<String> evaluateText(String text) {
        List<String> issues = new ArrayList<>();
        if (text == null || text.isBlank()) {
            issues.add("content is blank");
            return issues;
        }
        INTERNAL_MARKERS.stream()
                .filter(text::contains)
                .forEach(marker -> issues.add("contains internal marker: " + marker));
        return issues;
    }
}
```

```java
package org.example.ggbot.tool.ppt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.example.ggbot.tool.support.ResultQualityEvaluator;

public class PptSpecValidator {

    private static final List<String> DEMO_TITLES = List.of("背景与目标", "方案设计", "实施计划", "结论与下一步");
    private final ResultQualityEvaluator evaluator = new ResultQualityEvaluator();

    public ValidationResult validate(PptSpec spec, String instruction) {
        List<String> errors = new ArrayList<>();
        if (spec == null) {
            errors.add("spec is null");
            return new ValidationResult(false, errors);
        }
        if (spec.getTitle() == null || spec.getTitle().isBlank()) {
            errors.add("title is blank");
        }
        if (spec.getSlides() == null || spec.getSlides().isEmpty()) {
            errors.add("slides are empty");
        } else {
            if (spec.getSlides().size() < 4 || spec.getSlides().size() > 8) {
                errors.add("slides size should be between 4 and 8");
            }
            int demoTitleHits = 0;
            for (SlideSpec slide : spec.getSlides()) {
                if (slide.getTitle() == null || slide.getTitle().isBlank()) {
                    errors.add("slide title is blank");
                }
                if (slide.getBullets() == null || slide.getBullets().isEmpty()) {
                    errors.add("slide bullets are empty");
                }
                String combined = (slide.getTitle() == null ? "" : slide.getTitle()) + "\n" + String.join("\n", slide.getBullets());
                errors.addAll(evaluator.evaluateText(combined));
                if (DEMO_TITLES.contains(slide.getTitle())) {
                    demoTitleHits++;
                }
            }
            if (demoTitleHits >= 3 && !isProjectTopic(instruction)) {
                errors.add("matched fixed demo template");
            }
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }

    private boolean isProjectTopic(String instruction) {
        String normalized = instruction == null ? "" : instruction.toLowerCase(Locale.ROOT);
        return normalized.contains("项目") || normalized.contains("方案") || normalized.contains("mvp") || normalized.contains("接口");
    }

    public record ValidationResult(boolean valid, List<String> errors) {
    }
}
```

- [ ] **Step 4: Run validator tests to verify they pass**

Run: `mvn -Dtest=PptSpecValidatorTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/ggbot/tool/ppt/PptSpecValidator.java src/main/java/org/example/ggbot/tool/support/ResultQualityEvaluator.java src/test/java/org/example/ggbot/tool/ppt/PptSpecValidatorTest.java
git commit -m "feat: add ppt spec validation"
```

### Task 3: Fix Dependency Materialization and Artifact Rendering

**Files:**
- Create: `src/main/java/org/example/ggbot/tool/support/ArtifactContentExtractor.java`
- Modify: `src/main/java/org/example/ggbot/agent/execution/DefaultExecutor.java`
- Create: `src/test/java/org/example/ggbot/tool/support/ArtifactContentExtractorTest.java`
- Modify: `src/test/java/org/example/ggbot/agent/execution/DefaultExecutorTest.java`

- [ ] **Step 1: Write the failing readable-content tests**

```java
package org.example.ggbot.tool.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.model.Slide;
import org.junit.jupiter.api.Test;

class ArtifactContentExtractorTest {

    private final ArtifactContentExtractor extractor = new ArtifactContentExtractor();

    @Test
    void shouldReturnMarkdownForDocumentArtifact() {
        ToolResult result = new ToolResult(ToolName.GENERATE_DOC, true, "文档已生成", new DocumentArtifact("红军长征", "# 红军长征"));

        assertThat(extractor.extract(result)).isEqualTo("# 红军长征");
    }

    @Test
    void shouldRenderSlidesForPptArtifact() {
        ToolResult result = new ToolResult(ToolName.GENERATE_PPT, true, "PPT 已生成", new PptArtifact(
                "红军长征",
                List.of(new Slide(1, "历史背景", List.of("第五次反围剿失利", "战略转移开始", "形势严峻"))),
                "generated/pptx/demo.pptx"
        ));

        String extracted = extractor.extract(result);

        assertThat(extracted).contains("历史背景");
        assertThat(extracted).doesNotContain("PptArtifact(");
    }
}
```

```java
@Test
void shouldMaterializeDependencyInstructionWithReadableArtifactContent() {
    SpringAiToolExecutor toolExecutor = mock(SpringAiToolExecutor.class);
    when(toolExecutor.execute(any(), any(), any(), any())).thenReturn(
            new ToolResult(ToolName.GENERATE_PPT, true, "ppt ok", "ppt ok")
    );
    DefaultExecutor executor = new DefaultExecutor(toolExecutor, new ArtifactContentExtractor());

    AgentState state = AgentState.initialize(...);
    Plan plan = new Plan();
    PlanStep docStep = new PlanStep("step-1", StepType.GENERATE_DOC, ToolName.GENERATE_DOC, "生成文档", "生成文档", List.of(), List.of(), "文档");
    docStep.markSuccess(new ToolResult(ToolName.GENERATE_DOC, true, "文档已生成", new DocumentArtifact("红军长征", "# 红军长征\n## 历史背景")));
    PlanStep pptStep = new PlanStep("step-2", StepType.GENERATE_PPT, ToolName.GENERATE_PPT, "生成PPT", "生成PPT", List.of("step-1"), List.of("step-1"), "PPT");
    plan.addStep(docStep);
    plan.addStep(pptStep);
    state.setCurrentPlan(plan);
    state.getIntermediateResults().put("step-1", docStep.getResult());

    executor.execute(state, plan);

    verify(toolExecutor).execute(eq(ToolName.GENERATE_PPT), contains("# 红军长征"), any(), any());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -Dtest=ArtifactContentExtractorTest,DefaultExecutorTest test`
Expected: FAIL because extractor does not exist and `DefaultExecutor` does not use it.

- [ ] **Step 3: Implement readable artifact extraction and wire executor**

```java
package org.example.ggbot.tool.support;

import java.util.stream.Collectors;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.model.DocumentArtifact;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.model.Slide;

public class ArtifactContentExtractor {

    private final ResultQualityEvaluator qualityEvaluator = new ResultQualityEvaluator();

    public String extract(ToolResult result) {
        if (result == null) {
            return "";
        }
        Object artifact = result.getArtifact();
        String content;
        if (artifact instanceof DocumentArtifact documentArtifact) {
            content = documentArtifact.getMarkdown();
        } else if (artifact instanceof PptArtifact pptArtifact) {
            content = renderPptArtifact(pptArtifact);
        } else if (artifact == null) {
            content = result.getSummary();
        } else {
            content = String.valueOf(result.getSummary());
        }
        return sanitize(content);
    }

    private String renderPptArtifact(PptArtifact artifact) {
        return artifact.getSlides().stream()
                .map(this::renderSlide)
                .collect(Collectors.joining("\n\n"));
    }

    private String renderSlide(Slide slide) {
        return slide.getPageNumber() + ". " + slide.getTitle() + "\n- " + String.join("\n- ", slide.getBullets());
    }

    private String sanitize(String text) {
        String value = text == null ? "" : text;
        for (String issueMarker : ResultQualityEvaluator.INTERNAL_MARKERS) {
            value = value.replace(issueMarker, "");
        }
        return value.trim();
    }
}
```

```java
public class DefaultExecutor implements Executor {

    private final SpringAiToolExecutor toolExecutor;
    private final ArtifactContentExtractor artifactContentExtractor;

    public DefaultExecutor(SpringAiToolExecutor toolExecutor, ArtifactContentExtractor artifactContentExtractor) {
        this.toolExecutor = toolExecutor;
        this.artifactContentExtractor = artifactContentExtractor;
    }

    private String materializeInstruction(PlanStep step, AgentState state) {
        if (step.getInputRefs().isEmpty()) {
            return step.getInstruction();
        }
        String dependencyContext = step.getInputRefs().stream()
                .map(ref -> {
                    Object result = state.getIntermediateResults().get(ref);
                    if (result instanceof ToolResult toolResult) {
                        return "%s: %s".formatted(ref, artifactContentExtractor.extract(toolResult));
                    }
                    return "%s: %s".formatted(ref, String.valueOf(result));
                })
                .collect(Collectors.joining("\n"));
        return step.getInstruction() + "\n\n前置结果：\n" + dependencyContext;
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -Dtest=ArtifactContentExtractorTest,DefaultExecutorTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/ggbot/tool/support/ArtifactContentExtractor.java src/main/java/org/example/ggbot/agent/execution/DefaultExecutor.java src/test/java/org/example/ggbot/tool/support/ArtifactContentExtractorTest.java src/test/java/org/example/ggbot/agent/execution/DefaultExecutorTest.java
git commit -m "fix: render readable artifact content for tool chaining"
```

### Task 4: Add PPTX Rendering

**Files:**
- Create: `src/main/java/org/example/ggbot/tool/ppt/PptxRenderer.java`
- Modify: `src/main/java/org/example/ggbot/tool/model/PptArtifact.java`
- Create: `src/test/java/org/example/ggbot/tool/ppt/PptxRendererTest.java`

- [ ] **Step 1: Write the failing renderer test**

```java
package org.example.ggbot.tool.ppt;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PptxRendererTest {

    @Test
    void shouldRenderRealPptxFile() throws Exception {
        PptxRenderer renderer = new PptxRenderer(Path.of("target/test-generated/pptx"));
        PptSpec spec = new PptSpec("红军长征", List.of(
                new SlideSpec(1, "历史背景", List.of("第五次反围剿失利", "战略转移开始", "形势严峻"), null),
                new SlideSpec(2, "主要事件", List.of("突破封锁线", "四渡赤水", "会师北上"), null),
                new SlideSpec(3, "重要会议", List.of("遵义会议召开", "确立正确领导", "扭转被动局面"), null),
                new SlideSpec(4, "历史意义", List.of("保存革命力量", "形成长征精神", "影响革命进程"), null)
        ));

        Path output = renderer.render(spec);

        assertThat(Files.exists(output)).isTrue();
        assertThat(Files.size(output)).isGreaterThan(0L);
        assertThat(output.getFileName().toString()).endsWith(".pptx");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=PptxRendererTest test`
Expected: FAIL because renderer does not exist yet.

- [ ] **Step 3: Implement minimal POI renderer and file-path artifact**

```java
package org.example.ggbot.tool.model;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class PptArtifact {
    private final String title;
    private final List<Slide> slides;
    private final String filePath;
}
```

```java
package org.example.ggbot.tool.ppt;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;

public class PptxRenderer {

    private final Path outputDirectory;

    public PptxRenderer(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public Path render(PptSpec spec) {
        try {
            Files.createDirectories(outputDirectory);
            Path output = outputDirectory.resolve("ppt-" + Instant.now().toEpochMilli() + "-" + UUID.randomUUID() + ".pptx");
            try (XMLSlideShow slideShow = new XMLSlideShow(); OutputStream stream = Files.newOutputStream(output)) {
                slideShow.setPageSize(new Dimension(960, 540));
                for (SlideSpec slideSpec : spec.getSlides()) {
                    XSLFSlide slide = slideShow.createSlide();
                    addTitle(slide, slideSpec.getTitle());
                    addBullets(slide, slideSpec.getBullets());
                }
                slideShow.write(stream);
            }
            return output;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to render PPTX file", ex);
        }
    }

    private void addTitle(XSLFSlide slide, String title) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(50, 35, 620, 60));
        XSLFTextParagraph paragraph = box.addNewTextParagraph();
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(title);
        run.setFontSize(24.0);
        run.setFontFamily("Microsoft YaHei");
    }

    private void addBullets(XSLFSlide slide, java.util.List<String> bullets) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle(70, 120, 600, 320));
        for (String bullet : bullets) {
            XSLFTextParagraph paragraph = box.addNewTextParagraph();
            paragraph.setBullet(true);
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(bullet);
            run.setFontSize(18.0);
            run.setFontFamily("Microsoft YaHei");
        }
    }
}
```

- [ ] **Step 4: Run renderer test to verify it passes**

Run: `mvn -Dtest=PptxRendererTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/ggbot/tool/model/PptArtifact.java src/main/java/org/example/ggbot/tool/ppt/PptxRenderer.java src/test/java/org/example/ggbot/tool/ppt/PptxRendererTest.java
git commit -m "feat: render ppt spec to pptx file"
```

### Task 5: Rebuild GeneratePptTool Around PptSpec

**Files:**
- Create: `src/main/java/org/example/ggbot/tool/ppt/PptSpecGenerationClient.java`
- Create: `src/main/java/org/example/ggbot/tool/ppt/SpringAiPptSpecGenerationClient.java`
- Create: `src/main/java/org/example/ggbot/tool/ppt/SemanticPptFallbackGenerator.java`
- Modify: `src/main/java/org/example/ggbot/tool/impl/GeneratePptTool.java`
- Create: `src/test/java/org/example/ggbot/tool/impl/GeneratePptToolTest.java`
- Modify: `src/test/java/org/example/ggbot/tool/SpringAiToolExecutorTest.java`

- [ ] **Step 1: Write the failing GeneratePptTool tests**

```java
package org.example.ggbot.tool.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.model.PptArtifact;
import org.example.ggbot.tool.ppt.PptSpecGenerationClient;
import org.example.ggbot.tool.ppt.PptSpecParser;
import org.example.ggbot.tool.ppt.PptSpecValidator;
import org.example.ggbot.tool.ppt.PptxRenderer;
import org.example.ggbot.tool.ppt.SemanticPptFallbackGenerator;
import org.example.ggbot.tool.support.ArtifactContentExtractor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class GeneratePptToolTest {

    @Test
    void shouldGeneratePptArtifactFromValidJson() {
        PptSpecGenerationClient client = Mockito.mock(PptSpecGenerationClient.class);
        when(client.generate(anyString())).thenReturn("""
                {
                  "title": "红军长征",
                  "slides": [
                    {"title": "长征概览", "bullets": ["重大历史事件", "战略转移过程", "汇报范围说明"]},
                    {"title": "历史背景", "bullets": ["反围剿失利", "根据地压力", "战略转移启动"]},
                    {"title": "重要会议", "bullets": ["遵义会议召开", "确立正确领导", "扭转被动局面"]},
                    {"title": "历史意义", "bullets": ["保存革命力量", "形成长征精神", "影响革命进程"]}
                  ]
                }
                """);

        GeneratePptTool tool = new GeneratePptTool(
                client,
                new PptSpecParser(new com.fasterxml.jackson.databind.ObjectMapper()),
                new PptSpecValidator(),
                new SemanticPptFallbackGenerator(),
                new PptxRenderer(Path.of("target/test-generated/ppt-tool")),
                new ArtifactContentExtractor()
        );

        ToolResult result = tool.execute("请基于红军长征生成汇报 PPT", context(), Map.of());

        assertThat(result.getToolName()).isEqualTo(ToolName.GENERATE_PPT);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getArtifact()).isInstanceOf(PptArtifact.class);
        assertThat(((PptArtifact) result.getArtifact()).getFilePath()).endsWith(".pptx");
    }

    @Test
    void shouldFallbackWhenJsonIsInvalidTwice() {
        PptSpecGenerationClient client = Mockito.mock(PptSpecGenerationClient.class);
        when(client.generate(anyString())).thenReturn("not json");

        GeneratePptTool tool = new GeneratePptTool(...);

        ToolResult result = tool.execute("请介绍红军长征的历史背景和重要战役", context(), Map.of());

        PptArtifact artifact = (PptArtifact) result.getArtifact();
        assertThat(result.isSuccess()).isTrue();
        assertThat(artifact.getTitle()).contains("红军长征");
        assertThat(artifact.getSlides()).extracting("title").doesNotContain("方案设计", "实施计划");
    }

    private AgentContext context() {
        return new AgentContext("task-1", "conversation-1", "user-1", AgentChannel.WEB, List.of(), Map.of());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -Dtest=GeneratePptToolTest,SpringAiToolExecutorTest test`
Expected: FAIL because new constructor flow and generation pipeline do not exist yet.

- [ ] **Step 3: Implement generation client, fallback, and tool**

```java
package org.example.ggbot.tool.ppt;

public interface PptSpecGenerationClient {
    String generate(String instruction);
}
```

```java
package org.example.ggbot.tool.ppt;

import org.example.ggbot.ai.ReliableChatService;
import org.springframework.stereotype.Component;

@Component
public class SpringAiPptSpecGenerationClient implements PptSpecGenerationClient {

    private static final String SYSTEM_PROMPT = """
            你正在为一个 Java Agent 系统生成 PPT 结构化内容。
            ...
            只输出严格 JSON。
            """;

    private final ReliableChatService chatService;

    public SpringAiPptSpecGenerationClient(ReliableChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public String generate(String instruction) {
        return chatService.chat(SYSTEM_PROMPT, instruction);
    }
}
```

```java
package org.example.ggbot.tool.ppt;

import java.util.List;

public class SemanticPptFallbackGenerator {

    public PptSpec generate(String instruction) {
        if (instruction != null && instruction.contains("历史")) {
            return new PptSpec(extractTitle(instruction), List.of(
                    new SlideSpec(1, "主题概览", List.of("说明事件主题", "概括核心脉络", "交代汇报范围"), null),
                    new SlideSpec(2, "历史背景", List.of("交代时代环境", "说明直接原因", "指出任务起点"), null),
                    new SlideSpec(3, "主要事件", List.of("梳理关键过程", "概括重要节点", "说明发展变化"), null),
                    new SlideSpec(4, "关键会议与战役", List.of("提炼重要会议", "提炼重要战役", "说明影响作用"), null),
                    new SlideSpec(5, "历史意义", List.of("总结历史价值", "概括长期影响", "提炼精神启示"), null)
            ));
        }
        return new PptSpec(extractTitle(instruction), List.of(
                new SlideSpec(1, "主题概览", List.of("概述主题范围", "说明核心问题", "交代汇报结构"), null),
                new SlideSpec(2, "核心内容", List.of("提炼关键概念", "归纳主要信息", "说明重点关系"), null),
                new SlideSpec(3, "关键要点", List.of("总结重要节点", "补充代表案例", "强调注意事项"), null),
                new SlideSpec(4, "总结", List.of("回顾主题重点", "提炼主要结论", "给出下一步建议"), null)
        ));
    }

    private String extractTitle(String instruction) {
        return instruction == null || instruction.isBlank() ? "主题汇报" : instruction.replace("请", "").trim();
    }
}
```

```java
public class GeneratePptTool {

    public ToolResult execute(String prompt, AgentContext context, Map<String, Object> parameters) {
        PptSpec spec = generateSpec(prompt, parameters);
        Path output = renderer.render(spec);
        PptArtifact artifact = new PptArtifact(
                spec.getTitle(),
                spec.getSlides().stream().map(slide -> new Slide(slide.getPageNumber(), slide.getTitle(), slide.getBullets())).toList(),
                output.toString()
        );
        return new ToolResult(ToolName.GENERATE_PPT, true, "已生成 PPTX 文件：" + output, artifact);
    }
}
```

- [ ] **Step 4: Run tool tests to verify they pass**

Run: `mvn -Dtest=GeneratePptToolTest,SpringAiToolExecutorTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/ggbot/tool/ppt src/main/java/org/example/ggbot/tool/impl/GeneratePptTool.java src/test/java/org/example/ggbot/tool/impl/GeneratePptToolTest.java src/test/java/org/example/ggbot/tool/SpringAiToolExecutorTest.java
git commit -m "feat: generate pptx from llm ppt spec"
```

### Task 6: Minimal Semantic Fixes for Doc and Modify Tools

**Files:**
- Modify: `src/main/java/org/example/ggbot/tool/impl/GenerateDocTool.java`
- Modify: `src/main/java/org/example/ggbot/tool/impl/ModifyPptTool.java`
- Modify: `src/test/java/org/example/ggbot/tool/SpringAiToolExecutorTest.java`

- [ ] **Step 1: Write the failing semantic tool assertions**

```java
@Test
void shouldGenerateTopicBasedDocumentInsteadOfProjectTemplate() {
    GenerateDocTool tool = new GenerateDocTool();

    ToolResult result = tool.execute("请生成一份关于红军长征的文档", context(), Map.of());

    DocumentArtifact artifact = (DocumentArtifact) result.getArtifact();
    assertThat(artifact.getTitle()).contains("红军长征");
    assertThat(artifact.getMarkdown()).doesNotContain("MVP", "接口", "用户价值");
}

@Test
void shouldModifyPptWithoutReturningDemoTemplate() {
    ModifyPptTool tool = ...;

    ToolResult result = tool.execute("在原有红军长征 PPT 上补充重要会议和重要战役", context(), Map.of());

    PptArtifact artifact = (PptArtifact) result.getArtifact();
    assertThat(artifact.getSlides()).extracting("title").doesNotContain("方案设计", "实施计划");
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -Dtest=SpringAiToolExecutorTest test`
Expected: FAIL because the old tools still return project-plan/demo template content.

- [ ] **Step 3: Implement minimal topic-based fallbacks**

```java
public class GenerateDocTool {

    public DocumentArtifact generateDoc(String prompt) {
        String title = inferTitle(prompt);
        String markdown = """
                # %s

                ## 概述
                %s

                ## 核心内容
                - 围绕主题提炼背景与关键脉络
                - 梳理主要内容和代表性信息
                - 提炼理解主题所需的重要要点

                ## 关键要点
                1. 从主题背景切入
                2. 展开主要事件或核心内容
                3. 总结影响、意义或结论

                ## 总结
                本文档围绕主题进行结构化整理，可直接作为后续 PPT 生成输入。
                """.formatted(title, prompt);
        return new DocumentArtifact(title, markdown);
    }
}
```

```java
public class ModifyPptTool {

    public ToolResult execute(String prompt, AgentContext context, Map<String, Object> parameters) {
        PptSpec spec = semanticPptFallbackGenerator.generate(prompt + "\n请保留原主题并根据修改要求优化页面结构。");
        Path output = renderer.render(spec);
        PptArtifact artifact = toArtifact(spec, output);
        return new ToolResult(ToolName.MODIFY_PPT, true, "已根据修改要求重新生成 PPTX 文件：" + output, artifact);
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -Dtest=SpringAiToolExecutorTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/ggbot/tool/impl/GenerateDocTool.java src/main/java/org/example/ggbot/tool/impl/ModifyPptTool.java src/test/java/org/example/ggbot/tool/SpringAiToolExecutorTest.java
git commit -m "fix: make doc and modify tools topic-aware"
```

### Task 7: Full Verification

**Files:**
- Modify as needed: all touched source and test files above

- [ ] **Step 1: Run targeted tool-chain test suite**

Run: `mvn -Dtest=PptSpecParserTest,PptSpecValidatorTest,ArtifactContentExtractorTest,PptxRendererTest,GeneratePptToolTest,DefaultExecutorTest,SpringAiToolExecutorTest test`
Expected: PASS

- [ ] **Step 2: Run broader regression check for agent execution**

Run: `mvn -Dtest=PlannerServiceTest,LangGraphAgentRunnerTest,AgentServiceTest test`
Expected: PASS

- [ ] **Step 3: Fix any failures with minimal scope**

```text
Only adjust code that is directly affected by the PPT tool-chain change.
Do not refactor PlannerService, AgentGraphFactory, or unrelated web controllers.
```

- [ ] **Step 4: Run the full verification commands again**

Run:
- `mvn -Dtest=PptSpecParserTest,PptSpecValidatorTest,ArtifactContentExtractorTest,PptxRendererTest,GeneratePptToolTest,DefaultExecutorTest,SpringAiToolExecutorTest test`
- `mvn -Dtest=PlannerServiceTest,LangGraphAgentRunnerTest,AgentServiceTest test`
Expected: both PASS

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/java src/test/java
git commit -m "test: verify ppt spec rendering toolchain"
```
