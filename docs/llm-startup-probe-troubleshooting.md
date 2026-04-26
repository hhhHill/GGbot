# LLM Startup Probe 问题复盘

## 现象

在已经提供以下配置的情况下：

- `SPRING_AI_OPENAI_BASE_URL`
- `SPRING_AI_OPENAI_API_KEY`
- `SPRING_AI_OPENAI_MODEL`

启动日志仍然持续出现：

- `LLM startup probe skipped: model is not configured`

同时，配置诊断日志显示属性值都已经被正确解析：

- `spring.ai.openai.base-url`
- `spring.ai.openai.chat.options.model`
- `spring.ai.openai.api-key`

这说明问题不是 `.env` 未生效，也不是配置名写错。

## 第一层问题：`ChatClient` Bean 缺失

为了区分“配置存在”和“Bean 真正创建成功”，排查过程中给启动日志增加了 Bean 诊断：

- `ChatModel`
- `ChatClient`
- `ChatClient.Builder`

实际观察结果是：

- `ChatModel=<present> org.springframework.ai.openai.OpenAiChatModel`
- `ChatClient.Builder=<present> org.springframework.ai.chat.client.DefaultChatClientBuilder`
- `ChatClient=<missing>`

这说明：

1. Spring AI OpenAI 模型自动装配已经成功
2. `ChatClient.Builder` 已经可用
3. 只有项目自定义的 `ChatClient` Bean 没有注册成功

因此，`model is not configured` 在这里的真实含义不是“模型参数没配”，而是：

- `SpringAiChatService` 注入不到 `ChatClient`
- `chatService.isAvailable()` 返回 `false`
- 启动探针把它归类为“模型未配置”

## 根因一：`@ConditionalOnBean(ChatModel.class)` 判断时机不合适

当时 `SpringAiConfig` 中的实现是：

- `ChatClient` Bean 通过 `@ConditionalOnBean(ChatModel.class)` 保护

原始意图是：

- 只有存在模型 Bean 时才创建 `ChatClient`
- 避免在未配置模型时影响启动

但在当前 Spring Boot / Spring AI 自动装配时序下，这个条件没有按预期命中，结果变成：

- `ChatModel` 最终确实存在
- 但 `chatClient(...)` 方法在条件判断阶段被跳过了

于是出现了一个容易误判的状态：

- 配置有值
- `ChatModel` 存在
- `ChatClient.Builder` 存在
- `ChatClient` 却缺失

## 第二层问题：去掉条件后暴露出循环依赖

当 `@ConditionalOnBean(ChatModel.class)` 去掉以后，`ChatClient` 终于开始正常创建，但应用立刻因为循环依赖启动失败。

循环链路如下：

`SummarizeTool`
-> `SpringAiChatService`
-> `ChatClient`
-> `OpenAiChatModel`
-> Spring AI Tool Calling 自动配置
-> `ToolCallbackProvider`
-> `SummarizeTool`

也就是说：

- `SummarizeTool` 依赖 `SpringAiChatService`
- `SpringAiChatService` 依赖 `ChatClient`
- `ChatClient` 初始化时又接入了 Spring AI 的 tool calling
- `ToolCallbackProvider` 又把 `SummarizeTool` 注册成了一个 tool object

最终形成“模型初始化依赖工具，工具又反过来依赖模型”的闭环。

## 根因二：`SummarizeTool` 被错误地注册成 Spring AI Tool Callback

`SummarizeTool` 在当前项目里的真实角色是：

- Web Chat 场景下的内部聊天执行入口
- 内部会调用 `SpringAiChatService`

它不是一个纯工具。

而 `ToolCallbackProvider` 更适合暴露的是这类对象：

- 无需再依赖 LLM 的纯工具
- 例如生成文档、生成 PPT、修改 PPT

把 `SummarizeTool` 注册进 Spring AI tool calling 的问题在于：

- 它自己就会再次调用 LLM
- 这会把聊天层和工具层反向绑死
- 很容易形成初始化循环依赖

## 最终修复

本次最终用了两步修复：

### 1. 无条件创建 `ChatClient`

将 `SpringAiConfig` 中的 `ChatClient` Bean 改为直接创建：

- 不再使用 `@ConditionalOnBean(ChatModel.class)`

原因：

- 既然 `ChatClient.Builder` 已经由 Spring AI 自动装配提供
- 那么 `ChatClient` 的创建条件已经足够明确
- 再加一层 `ChatModel` 条件反而引入了装配时序风险

### 2. 从 `ToolCallbackProvider` 中移除 `SummarizeTool`

`ToolCallbackProvider` 最终只保留：

- `GenerateDocTool`
- `GeneratePptTool`
- `ModifyPptTool`

不再注册：

- `SummarizeTool`

这样以后：

- 聊天链路仍然是 `SummarizeTool -> SpringAiChatService -> ChatClient`
- Spring AI tool calling 只依赖纯工具
- 不再反向依赖聊天层

循环依赖因此被彻底断开。

## 这次排查中学到的点

### 1. 配置值存在，不等于 Bean 一定存在

看到：

- `base-url`
- `model`
- `api-key`

都已经解析出来，只能证明属性绑定成功，不能证明：

- `ChatModel` 已创建
- `ChatClient` 已创建
- 依赖链已经完整可用

### 2. 排查 Spring 启动问题时，Bean 诊断比属性诊断更关键

这次真正把问题锁定住的不是配置日志，而是 Bean 诊断：

- `ChatModel`
- `ChatClient`
- `ChatClient.Builder`

没有这层信息时，很容易把问题误判成：

- key 无效
- base-url 错了
- `.env` 没加载

### 3. `@ConditionalOnBean` 不是“更稳”，很多时候反而会制造时序问题

尤其当目标 Bean 本身也依赖自动配置时：

- 条件判断和最终 Bean 是否存在，不一定发生在同一时刻

因此：

- 只有在确实需要做“可选能力开关”时再用
- 否则优先依赖方法参数本身的注入约束

### 4. Tool Callback 应该尽量是纯工具

如果一个 Bean：

- 内部还要再次调用 LLM
- 或者依赖 `ChatClient`

那它通常不应该再被注册成 Spring AI 的 tool callback。

否则很容易出现：

- 初始化循环依赖
- 职责边界混乱
- 后续排查困难

## 当前正确结构

现在项目中的结构应理解为：

- `SpringAiChatService`：统一聊天模型调用入口
- `SummarizeTool`：项目内部聊天执行工具，依赖 `SpringAiChatService`
- `ToolCallbackProvider`：只暴露纯工具，不包含 `SummarizeTool`

这样既保留了当前 Web Chat 主链路，也避免了 Spring AI tool calling 和聊天层互相缠绕。

## 附注

早期文档 [spring-ai-bootstrap-step1.md](/D:/GGbot/docs/spring-ai-bootstrap-step1.md:1) 中曾记录：

- `ChatClient` 使用 `@ConditionalOnBean(ChatModel.class)` 保护

这个设计在当前实现和当前依赖版本下已经被证明不合适，应以当前代码和本复盘为准。
