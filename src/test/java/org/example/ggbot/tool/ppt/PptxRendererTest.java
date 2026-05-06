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
                new SlideSpec(1, "历史背景", "阶段背景", List.of("第五次反围剿失利", "战略转移开始", "形势严峻", "被迫转移"), notes()),
                new SlideSpec(2, "主要事件", "过程梳理", List.of("突破封锁线", "四渡赤水", "会师北上", "战略调整"), notes()),
                new SlideSpec(3, "重要会议", "转折节点", List.of("遵义会议召开", "确立正确领导", "扭转被动局面", "统一方向"), notes()),
                new SlideSpec(4, "历史意义", "长期影响", List.of("保存革命力量", "形成长征精神", "影响革命进程", "奠定基础"), notes())
        ));

        Path output = renderer.render(spec);

        assertThat(Files.exists(output)).isTrue();
        assertThat(Files.size(output)).isGreaterThan(0L);
        assertThat(output.getFileName().toString()).endsWith(".pptx");
    }

    private static String notes() {
        return "本页讲稿用于补充历史背景、关键过程、重要会议和长期影响，确保展示时能够把逻辑链条和事实依据完整说明。";
    }
}
