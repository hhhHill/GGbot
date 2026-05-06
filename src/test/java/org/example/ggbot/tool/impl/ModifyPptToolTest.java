package org.example.ggbot.tool.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.example.ggbot.tool.ToolResult;
import org.example.ggbot.tool.ppt.PptSpec;
import org.example.ggbot.tool.ppt.PptxRenderer;
import org.example.ggbot.tool.ppt.SemanticPptFallbackGenerator;
import org.example.ggbot.tool.ppt.SlideSpec;
import org.junit.jupiter.api.Test;

class ModifyPptToolTest {

    @Test
    void shouldLoadModifyInstructionFromRepository() {
        SemanticPptFallbackGenerator generator = mock(SemanticPptFallbackGenerator.class);
        PptxRenderer renderer = mock(PptxRenderer.class);
        ClasspathPromptRepository promptRepository = mock(ClasspathPromptRepository.class);
        when(promptRepository.load("ppt-modify-instruction.txt", Map.of("prompt", "把第二页改成时间线"))).thenReturn(
                "把第二页改成时间线\n请保留原主题并根据修改要求优化页面结构。");
        when(generator.generate("把第二页改成时间线\n请保留原主题并根据修改要求优化页面结构。")).thenReturn(
                new PptSpec("测试", List.of(new SlideSpec(1, "封面", "概览", List.of("a", "b", "c", "d"), "本页讲稿用于说明封面主题和修改后的整体结构。"))));
        when(renderer.render(org.mockito.ArgumentMatchers.any(PptSpec.class))).thenReturn(Path.of("target/test-generated/modify.pptx"));

        ModifyPptTool tool = new ModifyPptTool(generator, renderer, promptRepository);

        ToolResult result = tool.execute("把第二页改成时间线", null, Map.of());

        assertThat(result.isSuccess()).isTrue();
        verify(promptRepository).load("ppt-modify-instruction.txt", Map.of("prompt", "把第二页改成时间线"));
    }
}
