package org.example.ggbot.tool.ppt;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class SemanticPptFallbackGenerator {

    public PptSpec generate(String instruction) {
        String title = extractTitle(instruction);
        String normalized = instruction == null ? "" : instruction.toLowerCase(Locale.ROOT);
        int slideCount = detectSlideCount(normalized);
        if (isHistoryTopic(normalized)) {
            return historyFallback(title, slideCount);
        }
        if (isTechnologyTopic(normalized)) {
            return technologyFallback(title, slideCount);
        }
        return genericFallback(title, slideCount);
    }

    private boolean isHistoryTopic(String normalized) {
        return normalized.contains("历史")
                || normalized.contains("事件")
                || normalized.contains("会议")
                || normalized.contains("战役")
                || normalized.contains("意义")
                || normalized.contains("背景");
    }

    private boolean isTechnologyTopic(String normalized) {
        return normalized.contains("架构")
                || normalized.contains("技术")
                || normalized.contains("系统")
                || normalized.contains("流程")
                || normalized.contains("实现");
    }

    private String extractTitle(String instruction) {
        if (instruction == null || instruction.isBlank()) {
            return "主题汇报";
        }
        String normalized = instruction
                .replace("先生成一份关于", "")
                .replace("请生成一份关于", "")
                .replace("请基于", "")
                .replace("请介绍", "")
                .replace("生成一个", "")
                .replace("生成", "")
                .replace("再基于文档生成一个汇报 PPT", "")
                .replace("PPT", "")
                .replace("ppt", "")
                .trim();
        return normalized.isBlank() ? "主题汇报" : normalized;
    }

    private int detectSlideCount(String instruction) {
        if (instruction.contains("brief")) {
            return 7;
        }
        if (instruction.contains("detailed")) {
            return 5;
        }
        return 6;
    }

    private PptSpec historyFallback(String title, int slideCount) {
        List<SlideSpec> slides = new java.util.ArrayList<>();
        slides.add(slide(1, "主题概览", "历史主题范围", "说明历史主题范围", "概括事件核心脉络", "交代本次汇报结构", "提出核心判断"));
        slides.add(slide(2, "历史背景", "时代环境", "交代时代环境", "说明直接原因", "指出任务起点", "解释背景压力"));
        slides.add(slide(3, "主要事件", "过程梳理", "梳理关键过程", "概括重要节点", "说明发展变化", "提炼关键转折"));
        slides.add(slide(4, "关键会议与战役", "核心节点", "提炼重要会议", "提炼重要战役", "说明关键影响", "概括战略价值"));
        slides.add(slide(5, "历史意义", "长期影响", "总结历史价值", "概括长期影响", "提炼现实启示", "形成总结判断"));
        if (slideCount >= 6) {
            slides.add(slide(6, "代表人物与决策", "组织调整", "概括核心领导变化", "提炼重要决策逻辑", "解释组织作用", "说明历史启发"));
        }
        if (slideCount >= 7) {
            slides.add(slide(7, "总结与启示", "现实意义", "回顾主要结论", "说明精神价值", "联系现实场景", "提出学习启示"));
        }
        return new PptSpec(title, slides);
    }

    private PptSpec technologyFallback(String title, int slideCount) {
        List<SlideSpec> slides = new java.util.ArrayList<>();
        slides.add(slide(1, "主题概览", "问题与目标", "说明技术主题范围", "概括核心问题", "交代汇报结构", "明确分析目标"));
        slides.add(slide(2, "核心概念", "基础定义", "解释关键概念", "说明组成要素", "明确边界定义", "补充上下游关系"));
        slides.add(slide(3, "架构与流程", "核心机制", "描述整体架构", "梳理关键流程", "指出关键节点", "说明协同关系"));
        slides.add(slide(4, "应用场景", "落地路径", "列举典型场景", "说明适用条件", "总结主要收益", "补充约束条件"));
        slides.add(slide(5, "优缺点分析", "价值判断", "概括主要优势", "说明潜在限制", "对比替代方案", "提示适用边界"));
        if (slideCount >= 6) {
            slides.add(slide(6, "落地建议", "实施思路", "分阶段推进建设", "明确能力依赖", "设置验证指标", "补充风险控制"));
        }
        if (slideCount >= 7) {
            slides.add(slide(7, "总结", "结论收束", "回顾核心判断", "强调适用价值", "指出下一步方向", "形成行动建议"));
        }
        return new PptSpec(title, slides);
    }

    private PptSpec genericFallback(String title, int slideCount) {
        List<SlideSpec> slides = new java.util.ArrayList<>();
        slides.add(slide(1, "主题概览", "范围说明", "概述主题范围", "说明核心问题", "交代汇报结构", "明确分析目标"));
        slides.add(slide(2, "背景与现状", "理解基础", "说明背景脉络", "概括当前现状", "提炼核心矛盾", "补充理解前提"));
        slides.add(slide(3, "核心内容", "主体分析", "提炼关键概念", "归纳主要信息", "说明重点关系", "补充典型例子"));
        slides.add(slide(4, "关键案例", "具体支撑", "总结重要节点", "补充代表案例", "强调注意事项", "解释实际影响"));
        slides.add(slide(5, "结论与建议", "行动方向", "回顾主题重点", "提炼主要结论", "给出下一步建议", "说明预期效果"));
        if (slideCount >= 6) {
            slides.add(slide(6, "补充分析", "延展说明", "补充相关背景", "解释变化趋势", "提示潜在风险", "强调实施条件"));
        }
        if (slideCount >= 7) {
            slides.add(slide(7, "总结提升", "整体收束", "回顾核心判断", "强化价值结论", "连接实际应用", "形成最终建议"));
        }
        return new PptSpec(title, slides);
    }

    private SlideSpec slide(int pageNumber, String title, String subtitle, String bullet1, String bullet2, String bullet3, String bullet4) {
        return new SlideSpec(
                pageNumber,
                title,
                subtitle,
                List.of(bullet1, bullet2, bullet3, bullet4),
                """
                本页讲稿用于补充背景、逻辑、案例、影响和结论，帮助讲述者在展示时把条目之间的关系讲清楚，并把主题的关键判断完整展开，避免只停留在概念和标题层面。
                讲述时还应进一步说明每个要点之间的因果联系、阶段变化和现实启示，让这一页能够独立支撑完整表达。
                同时需要交代这一页与前后页面的衔接关系、核心事实依据以及对整体主题的支撑作用，使演示过程具有连贯性和说服力。
                """.replace("\n", "")
        );
    }
}
