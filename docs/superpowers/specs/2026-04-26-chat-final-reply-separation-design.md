# Chat Final Reply Separation Design

## 背景

当前项目已经支持 `CHAT` 模式走多轮 Agent 主循环：

- `plan`
- `execute`
- `reflect`
- `replan`

这符合 AI-native 项目的方向，因为未来 `CHAT` 场景不应该只是“单轮问答”，还需要支持：

- 导出聊天记录
- 执行操作型命令
- 替代部分传统 UI 点击行为

因此，本次设计的目标不是把 `CHAT` 降级成单轮执行，而是保留内部多轮智能，同时让用户只看到一次最终结果。

## 问题

当前实现中，`AgentState.update(...)` 会把每轮执行的 `observation` 直接追加到 `finalReply`。

这导致：

1. `finalReply` 实际上变成“执行过程拼接稿”
2. 一旦 `CHAT` 进入多轮循环，前端拿到的 `replyText` 就会包含多轮中间结果
3. 用户看到的是多个阶段性回答的叠加，而不是一次整理后的单轮答复

这破坏了用户体验，也让后续在 `CHAT` 模式下引入动作型能力时更难控制输出边界。

## 目标

本次设计要同时满足两个条件：

1. 保留 `CHAT` 模式的多轮 Agent 循环能力
2. Web / Feishu / API 用户最终只看到一条最终答复

具体要求：

- 中间思考过程和执行记录仍然要保留
- 中间 `observation` 不直接暴露给用户
- 最终回复只在任务结束时生成一次
- 将来 `CHAT` 中出现工具调用或多轮补强时，仍然只返回一个面向用户的最终结果

## 非目标

本次设计不做以下扩展：

- 不实现流式输出
- 不新增前端调试面板
- 不修改 Planner 的规则体系
- 不新增 LLM Planner / LLM Reflector
- 不改变现有工具能力边界

## 方案选择

### 方案 A：保留多轮循环，分离“执行观察”和“最终回复”

核心思想：

- `executionHistory` 保存完整内部过程
- `finalReply` 不再在每轮执行时累加
- 在任务结束时统一决策一次用户可见回复

优点：

- 完全保留 AI-native 的多轮能力
- 输出边界清晰
- 容易扩展到未来的动作型 `CHAT`

缺点：

- 需要调整 `AgentState` 的职责
- 需要新增最终回复决策逻辑

### 方案 B：继续累加中间结果，结束前再压缩成单条回复

优点：

- 表面改动较小

缺点：

- 用户输出状态仍然先被污染
- 结构不干净
- 后续工具一多会继续泄漏内部过程

### 方案 C：为 `CHAT` 单独建立“思考轨”和“显示轨”事件流

优点：

- 结构最清晰
- 后续扩展流式输出和调试 UI 很自然

缺点：

- 对当前项目来说过重
- 超出本次修复范围

### 结论

采用方案 A。

## 设计

### 1. 职责重分层

当前职责存在混杂：

- `Executor` 执行步骤
- `Reflector` 决策是否继续
- `AgentState.update(...)` 同时承担“记录执行过程”和“构造最终用户回复”

问题就在最后一项。

本次调整后：

- `Executor`：只负责执行和产出 `ToolResult`
- `Reflector`：只负责判断 `done / needReplan / retry`
- `AgentState`：只保存状态、执行历史、中间结果
- `FinalReplyResolver`：只负责在任务结束时生成用户最终答复

### 2. `AgentState` 调整

`AgentState.update(...)` 保留以下职责：

- 记录 `executionHistory`
- 记录 `intermediateResults`
- 更新 `done`
- 更新上下文状态

`AgentState.update(...)` 不再做：

- 将 `observation` 直接拼接进 `finalReply`

也就是说，`finalReply` 不再是“循环中持续追加”的字段，而是“结束时一次性赋值”的字段。

### 3. 新增 `FinalReplyResolver`

新增一个独立组件，例如：

- `org.example.ggbot.agent.reply.FinalReplyResolver`

职责：

- 在任务结束时，从 `AgentState` 中提取最适合用户看到的结果

初版策略保持简单稳定：

1. 取最后一个成功执行记录
2. 如果该记录存在适合展示的 `ToolResult.summary`，优先使用它
3. 如果 `observation` 更适合展示，则使用 `observation`
4. 如果没有有效结果，则返回保底文案

设计约束：

- 只取一个最终结果
- 不拼接多轮历史
- 不直接回放整个内部过程

### 4. `AgentRunner` 调整

当前 `AgentRunner.run(...)` 在循环中不断更新状态，最后直接返回 `AgentState`。

调整后：

- 仍保留现有循环和 replan 机制
- 在循环退出后，统一调用 `FinalReplyResolver`
- 将解析结果一次性写入 `state.finalReply`

这样：

- 中间执行记录照常保留
- 用户可见回复只在最后生成一次

### 5. 前端与接口层影响

Web 前端当前只展示一次 `payload.data.replyText`，这层本身没有问题。

因此本次不需要修改：

- `app.js`
- `WebAgentController`
- `WebChatResponse`

只要后端最终返回的 `replyText` 是单条结果，前端自然不会再展示重复拼接内容。

## 数据流

调整后的数据流为：

1. 用户请求进入 `AgentService`
2. `AgentRunner` 执行多轮 `plan -> execute -> reflect -> replan`
3. 每轮结果进入 `executionHistory` / `intermediateResults`
4. 不在中途生成用户最终答复
5. 循环结束后调用 `FinalReplyResolver`
6. 生成单条 `finalReply`
7. `AgentService` 将该结果返回给前端

## 错误处理

以下场景需要明确行为：

### 1. 中间步骤多轮成功

- 只返回最后决策出的最终回复
- 不返回前面每轮的中间结果

### 2. 中间步骤失败后重试成功

- 仍只返回最终成功后的用户答复
- 失败记录保留在内部历史中

### 3. 最终没有可展示内容

- 返回保底文案
- 避免用户看到空回复

### 4. 达到最大迭代次数

- 仍调用 `FinalReplyResolver`
- 若没有有效结果，再回退到现有最大迭代保护文案

## 测试

至少覆盖以下测试：

### 1. `CHAT` 多轮执行只输出一次最终回复

验证：

- 即使内部发生多轮 `SUMMARIZE` 或 replan
- `finalReply` 也只取最终结果
- 不等于多轮 `observation` 拼接

### 2. 单轮 `CHAT` 正常输出

验证：

- 单次成功执行后仍能正常返回结果

### 3. 非聊天模式不回归

验证：

- `CREATE_DOC`
- `CREATE_PPT`
- `CREATE_DOC_AND_PPT`

的结果输出行为不被破坏。

### 4. 无结果时的保底文案

验证：

- 当没有合适用户可见结果时
- 系统返回稳定的默认答复

## 风险

### 1. `observation` 与 `summary` 的语义尚不完全分离

当前项目里有些工具把二者设置成相同内容。初版可以先按“最后有效结果”处理，但后续如果工具越来越多，可能需要进一步明确：

- `observation` 偏内部执行记录
- `summary` 偏用户可见摘要

### 2. 旧测试可能默认依赖拼接行为

如果现有测试隐式依赖“多轮结果直接累加到 `finalReply`”，实现后需要同步调整。

## 实施建议

建议按以下顺序改：

1. 先补测试，锁定“多轮内部执行但单条最终回复”的目标行为
2. 引入 `FinalReplyResolver`
3. 修改 `AgentState.update(...)`，去掉对 `finalReply` 的中途累加
4. 修改 `AgentRunner.run(...)`，在循环结束后统一生成最终回复
5. 回归验证 `CHAT` 与文档 / PPT 模式

## 结论

本次改动的核心不是“减少 `CHAT` 的智能度”，而是把：

- 内部思考过程

和

- 面向用户的最终答复

明确分层。

这样可以保留 AI-native 多轮能力，同时保证用户体验始终是一次请求只得到一条清晰结果。
