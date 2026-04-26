# Spring AI 迁移实施记录 - 第 4 步

## 目标

将项目的 `memory` 层从“自定义 `ConcurrentHashMap + List<String>` 存储”迁移到 Spring AI `ChatMemory`。

这一阶段的重点是：

- 保留 `ConversationMemoryService` 作为项目语义层
- 底层实现改为委托 Spring AI `ChatMemory`
- 继续兼容当前 `AgentState` 使用的字符串历史格式
- 为下一步接入 `MessageChatMemoryAdvisor` 预留 `Message` 级访问能力

## 设计

### 保留的部分

- `ConversationMemoryService`

原因：

- 这是项目领域层与基础设施层之间的隔离边界
- `AgentService` 不应该直接被 Spring AI API 污染

### 删除的部分

- `InMemoryConversationMemoryService`

原因：

- 它重复实现了会话历史存储
- Spring AI 已提供 `ChatMemory`

### 新结构

- 新增 `SpringAiConversationMemoryService`
  - 内部持有 `ChatMemory`
  - `appendUserMessage(...)` 写入 `UserMessage`
  - `appendAgentMessage(...)` 写入 `AssistantMessage`
  - `getConversationHistory(...)` 将 `Message` 映射回当前项目使用的 `USER:` / `AGENT:` 字符串
  - `getMessages(...)` 直接暴露底层 `Message` 列表，供后续 advisor 使用

## 当前边界

这一阶段完成后：

- memory 底层已经切到 Spring AI
- 但 `AgentState` 暂时仍保留字符串历史表示，避免一次性改动过大

## 下一步

下一步应该进入：

- Spring AI 迁移第 5 步：让 `Executor` / `AgentRunner` 开始消费 Spring AI memory 与 tool context
