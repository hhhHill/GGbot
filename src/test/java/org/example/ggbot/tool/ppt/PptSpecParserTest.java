package org.example.ggbot.tool.ppt;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PptSpecParserTest {

    private final PptSpecParser parser = new PptSpecParser(new ObjectMapper());

    @Test
    void shouldParseStandardJson() {
        String json = """
                {
                  "title": "红军长征",
                  "slides": [
                    {
                      "pageNumber": 1,
                      "subtitle": "阶段背景",
                      "title": "历史背景",
                      "bullets": ["第五次反围剿失利", "红军进行战略转移", "长征由此开始", "战略目标重新调整"],
                      "speakerNotes": "本页讲稿用于解释长征前夜的政治军事背景、失败原因与战略转移必要性，帮助听众理解后续事件为何发生。"
                    }
                  ]
                }
                """;

        PptSpec spec = parser.parse(json);

        assertThat(spec.getTitle()).isEqualTo("红军长征");
        assertThat(spec.getSlides()).hasSize(1);
        assertThat(spec.getSlides().getFirst().getTitle()).isEqualTo("历史背景");
        assertThat(spec.getSlides().getFirst().getSubtitle()).isEqualTo("阶段背景");
        assertThat(spec.getSlides().getFirst().getBullets()).hasSize(4);
        assertThat(spec.getSlides().getFirst().getSpeakerNotes()).contains("政治军事背景");
    }

    @Test
    void shouldStripJsonCodeFence() {
        String json = """
                ```json
                {
                  "title": "红军长征",
                  "slides": [
                    {
                      "subtitle": "关键节点",
                      "title": "遵义会议",
                      "bullets": ["确立新的军事领导", "成为战略转折点", "影响长征进程", "统一方向"],
                      "speakerNotes": "本页讲稿说明遵义会议如何扭转被动局面、重建领导核心并为长征后续阶段奠定基础。"
                    }
                  ]
                }
                ```
                """;

        PptSpec spec = parser.parse(json);

        assertThat(spec.getSlides().getFirst().getPageNumber()).isEqualTo(1);
        assertThat(spec.getSlides().getFirst().getTitle()).isEqualTo("遵义会议");
        assertThat(spec.getSlides().getFirst().getSubtitle()).isEqualTo("关键节点");
    }
}
