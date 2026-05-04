import { html } from "../lib/html.js";

const QUICK_ACTIONS = [
    {
        label: "生成文档",
        value: "请帮我生成一份完整的项目文档，包括项目背景、目标、功能设计、技术方案、实施步骤和风险评估。"
    },
    {
        label: "生成 PPT",
        value: "请帮我生成一份项目汇报 PPT 大纲，包括封面、背景、目标、方案、计划和总结。"
    },
    {
        label: "生成文档和 PPT",
        value: "请先帮我生成完整的项目文档，再基于文档内容生成对应的项目汇报 PPT 大纲。"
    },
    {
        label: "总结需求",
        value: "请帮我总结并结构化我的需求，整理成背景、目标、功能点、优先级和待确认问题。"
    }
];

export function QuickActions({ tone = "hero", onPick }) {
    return html`
        <div className=${`quick-actions quick-actions-${tone}`}>
            ${QUICK_ACTIONS.map((item) => html`
                <button
                    key=${item.label}
                    type="button"
                    className="quick-action-button"
                    onClick=${() => onPick(item.value)}
                >
                    ${item.label}
                </button>
            `)}
        </div>
    `;
}
