# Web Chat LLM 接入记录

## 目标

先把真实大模型对话接入到当前 Web MVP，但只覆盖 `CHAT` 意图。

## 设计

- 保持现有 `PlannerService` 不变
- `CHAT` 意图仍然映射到 `ToolName.SUMMARIZE`
- `SummarizeTool` 内部优先调用 `SpringAiChatService`
- 如果模型未配置或调用失败，则降级到模板化回复

## 当前边界

这一步只解决：

- 浏览器 MVP 的普通聊天问题能够走真实模型

这一步不解决：

- 文档 / PPT 生成走模型
- Planner / Reflector 的 LLM 化
- 流式输出

## 配置

使用现有 Spring AI 配置：

- `SPRING_AI_MODEL_CHAT`
- `SPRING_AI_OPENAI_BASE_URL`
- `SPRING_AI_OPENAI_API_KEY`
- `SPRING_AI_OPENAI_MODEL`
