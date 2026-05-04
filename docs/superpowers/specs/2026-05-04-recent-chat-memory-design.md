# Recent Chat Memory Design

**Goal**

为普通聊天工具增加“最近 5 轮对话记忆”，让 LLM 在处理当前输入时能看到最近的 USER / ASSISTANT 对话上下文。

**Scope**

- 复用现有 `AgentContext.conversationHistory`
- 新增 `MemoryManager` 负责截断、格式化、写回
- 新增 `ContextAwareChatService` 作为 `ReliableChatService` 之上的上下文感知适配层
- `SummarizeTool` 改为依赖 `ContextAwareChatService`
- 不修改 `ReliableChatService` / `SpringAiChatService` 公开接口

**Approach**

1. `AgentContext` 继续承载 `List<String>` 形式的对话历史，格式统一为 `USER: ...` / `ASSISTANT: ...`
2. `MemoryManager` 从 `AgentContext` 里提取最近 5 轮，即最多最近 10 条消息
3. `MemoryManager` 构建如下 prompt：

```text
=== 最近对话 ===
USER: ...
ASSISTANT: ...

=== 当前问题 ===
USER: ...
```

4. `ContextAwareChatService` 提供：
   - `chat(systemPrompt, currentInput, context)`
   - `stream(systemPrompt, currentInput, context)`
5. `ContextAwareChatService` 在调用 LLM 之前只读取旧历史构建 prompt，在得到完整回复后再写入：
   - `USER: currentInput`
   - `ASSISTANT: reply`

**Error Handling**

- `context == null` 时退化为原始 `ReliableChatService`
- `conversationHistory == null` 或为空时只构建 `=== 当前问题 ===`
- 只在完整回复非空时写入 assistant 记忆
- 避免连续重复写入同一条消息

**Testing**

- `MemoryManagerTest`
  - 空历史
  - 少于 5 轮
  - 超过 5 轮只保留最近 10 条
  - 避免重复写入
- `ContextAwareChatServiceTest`
  - `chat(...)` 使用拼装后的 fullPrompt
  - 成功后写回 USER / ASSISTANT
  - `stream(...)` 完成后写回 USER / ASSISTANT
- `SummarizeToolTest`
  - 改为验证调用 `ContextAwareChatService`
