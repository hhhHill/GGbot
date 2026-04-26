# Spring AI 迁移实施记录 - 第 2 步

## 目标

将项目中的模型调用入口从自定义 `llm.*` 抽象迁移到 Spring AI `ChatClient`。

这一阶段的重点是：

- 删除重复的 OpenAI-compatible HTTP 客户端
- 让项目内的聊天模型访问统一走 Spring AI
- 保持当前 Agent 主循环不被强行改写

## 本次改动

### 1. 删除旧 `llm` 包

删除了以下类：

- `llm.LlmClient`
- `llm.LlmProperties`
- `llm.OpenAiCompatibleLlmClient`

原因：

- 这一层只是在重复 Spring AI 已经提供的模型调用能力
- 当前主链路也没有真正依赖这组抽象，继续保留只会增加迁移噪音

### 2. 新增 `SpringAiChatService`

新增：

- `org.example.ggbot.ai.SpringAiChatService`

职责：

- 作为项目内统一的聊天模型调用入口
- 内部直接委托给 Spring AI `ChatClient`
- 在未配置模型时，返回可读的降级提示，保证项目当前开发阶段仍可启动联调

### 3. 清理旧配置入口

移除了：

- `app.llm.*`

原因：

- 模型配置已经迁移到 `spring.ai.*`
- 再保留一套项目私有配置会造成双轨配置和理解成本

当前仍然保留了环境变量兼容映射：

- `LLM_BASE_URL`
- `LLM_API_KEY`
- `LLM_MODEL`

它们通过 `spring.ai.openai.*` 的占位符兜底继续生效。

## 当前边界

这一阶段完成后：

- Spring AI 已接管模型调用入口
- 但 `tool`、`memory`、`Executor` 仍未迁移
- `AgentRunner` 主循环保持不变

## 下一步

下一步应该进入：

- Spring AI 迁移第 3 步：替换 `tool` 层

目标是：

- 删除 `ToolRegistry` / `Tool` / `ToolRequest` 这类自定义工具协议
- 把具体工具能力迁移为 Spring AI Tool Calling 可接入的形式
