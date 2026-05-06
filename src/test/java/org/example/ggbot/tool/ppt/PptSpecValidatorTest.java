package org.example.ggbot.tool.ppt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.example.ggbot.tool.support.PromptDetailLevel;
import org.junit.jupiter.api.Test;

class PptSpecValidatorTest {

    private final PptSpecValidator validator = new PptSpecValidator();

    @Test
    void shouldAcceptValidHistoryTopicDeck() {
        PptSpec spec = new PptSpec("红军长征", List.of(
                new SlideSpec(1, "长征概览", "主题与范围", List.of("重大历史事件", "战略转移过程", "汇报范围说明", "核心主线概括"), longNotes()),
                new SlideSpec(2, "历史背景", "危机形成", List.of("反围剿失利", "根据地压力", "战略转移启动", "军事路线争议"), longNotes()),
                new SlideSpec(3, "主要事件", "关键过程", List.of("突破封锁线", "四渡赤水", "会师北上", "沿途战略调整"), longNotes()),
                new SlideSpec(4, "历史意义", "长期影响", List.of("保存革命力量", "锻造领导核心", "形成长征精神", "影响革命进程"), longNotes()),
                new SlideSpec(5, "总结提升", "经验归纳", List.of("战略转折形成", "组织体系重建", "精神价值沉淀", "现实启示明确"), longNotes())
        ));

        PptSpecValidator.ValidationResult result = validator.validate(spec, "请生成红军长征汇报 PPT", PromptDetailLevel.DETAILED, 20);

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void shouldRejectInternalObjectStringsAndDemoTemplate() {
        PptSpec spec = new PptSpec("汇报演示稿", List.of(
                new SlideSpec(1, "背景与目标", "概述", List.of("ToolResult(", "artifact=", "success=", "org.example"), null),
                new SlideSpec(2, "方案设计", "方案", List.of("模块拆分", "接口", "MVP", "业务目标"), longNotes()),
                new SlideSpec(3, "实施计划", "执行", List.of("里程碑", "资源与风险", "数据协作流程", "阶段规划"), longNotes()),
                new SlideSpec(4, "结论与下一步", "收尾", List.of("总结", "建议", "行动", "下一步"), longNotes()),
                new SlideSpec(5, "附录", "补充", List.of("补充1", "补充2", "补充3", "补充4"), longNotes())
        ));

        PptSpecValidator.ValidationResult result = validator.validate(spec, "请介绍红军长征", PromptDetailLevel.BRIEF, 6);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).isNotEmpty();
    }

    private static String longNotes() {
        return """
                本页讲稿围绕事件背景、核心过程、关键影响和现实启示展开，确保演讲时能够补充逻辑关系、事实依据和总结判断，不会只停留在标题和条目层面。
                需要进一步交代每个要点之间的因果关系、阶段转折和长期影响，让听众能够理解这一页在整个主题中的分析价值。
                同时还要说明这一页如何承接前文、如何支撑后续结论，以及为什么这些事实足以构成完整判断，并形成能够直接口头表达的完整内容。
                """.replace("\n", "");
    }
}
