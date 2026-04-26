# Spring AI 迁移设计记录

## 目标

将当前项目从“自定义 AI 基础设施 + 自定义 Agent 编排”的实现方式，迁移为：

- **项目自身保留 Agent 决策循环**
- **Spring AI 接管模型、记忆、工具、上下文增强等 AI 基础设施**

迁移目标不是把项目改成“纯 Spring AI Demo”，而是让：

- `AgentRunner`
- `AgentState`
- `Planner`
- `Reflector`
- `RePlanner`

继续作为项目的核心领域层存在，同时避免继续重复实现 Spring AI 已经成熟提供的能力。

## 当前问题

目前项目已经形成了较完整的循环式 Agent 架构，但仍有一部分基础设施层是自定义实现：

- 自定义 `llm` 调用层
- 自定义 `tool` 协议与注册表
- 自定义 `memory` 维护方式

这些实现带来的问题：

- 和 Spring AI 的官方抽象重复
- 后续接入更多模型能力时维护成本高
- 工具调用、记忆管理、模型调用链缺乏统一基础设施
- 一旦继续扩展，容易在项目内部形成第二套 Agent 平台

## 保留层与替换层

### 必须保留的层

这些层属于项目自己的业务与 Agent 核心能力，不应该被 Spring AI 替代：

- `agent.*`
- `planner.*`
- `agent.reflection.*`
- `agent.replan.*`
- `task.*`
- `adapter.feishu.*`
- `adapter.web.*`

### 应交给 Spring AI 接管的层

这些层应改为使用 Spring AI 官方抽象：

- `llm.*`
- `tool.*` 中的协议与注册逻辑
- `memory.*` 的底层实现
- 一部分 prompt / advisor / memory 接入逻辑

## 迁移后目标边界

### 项目代码负责

- Agent 循环控制
- 规则规划
- 反思与重规划
- 业务任务管理
- Feishu / Web 接入

### Spring AI 负责

- 模型调用
- 会话记忆接入
- 工具声明与调用
- advisor 链
- 结构化输出
- 上下文增强

## 目标调用链

### Feishu 请求

```text
FeishuWebhookController
 -> AgentService
 -> AgentRunner
 -> Planner
 -> Executor
 -> ChatClient + ChatMemory + Tool Calling
 -> Reflector
 -> RePlanner
 -> AgentRunner loop
 -> AgentResult
 -> FeishuMessageClient
```

### Web 请求

```text
WebAgentController
 -> AgentService
 -> AgentRunner
 -> Planner
 -> Executor
 -> ChatClient + ChatMemory + Tool Calling
 -> Reflector
 -> RePlanner
 -> AgentRunner loop
 -> WebChatResponse
```

## 包级迁移策略

### 1. `agent`

保留，继续作为核心编排层。

改动方向：

- `Executor` 以后不再依赖自定义 `ToolRegistry`
- `AgentService` 不再依赖自定义 `LlmClient`
- `AgentState` 承接 Spring AI memory / tool execution / model output

### 2. `planner`

保留。

后续方向：

- 保留 `RuleBasedPlanner`
- 未来可新增 `LlmPlanner`
- 允许规则规划与模型规划并存

### 3. `adapter.feishu` / `adapter.web`

保留。

这些仍然负责协议适配，而不是 AI 基础设施。

### 4. `task`

保留。

任务生命周期管理不属于 Spring AI 的职责。

### 5. `tool`

保留工具能力，删除自定义工具协议。

将逐步删除：

- `Tool`
- `ToolRegistry`
- `ToolRequest`

保留：

- `tool.model.*`
- `tool.impl.*` 的业务能力本体

改造方式：

- 将工具实现改成 Spring AI `@Tool` 风格
- 或通过 Spring AI Tool Callback 注册

### 6. `memory`

保留目录，但实现改为委托给 Spring AI `ChatMemory`。

不建议直接把 `memory` 包删掉，因为项目仍然需要自己的语义边界。

### 7. `llm`

基本应整体退出历史舞台。

将逐步删除：

- `LlmClient`
- `OpenAiCompatibleLlmClient`
- 旧的模型配置对象

改为：

- 使用 Spring AI `ChatClient`
- 在 `config` 中完成相关 Bean 装配

### 8. `config`

保留并增强，成为 Spring AI 基础设施装配入口。

未来承接：

- `ChatClient` Bean
- `ChatMemory` Bean
- Spring AI tools 注册
- advisor 链注册
- 统一 AI 配置

## 迁移步骤

### 第一步：引入 Spring AI 依赖与配置

本步目标：

- 让 Spring AI 在项目中可用
- 但暂时不改 Agent 主链路

包括：

- 修改 `pom.xml`
- 增加 Spring AI 配置类
- 准备 `ChatClient.Builder`

### 第二步：替换 `llm` 层

本步目标：

- 删除自定义模型 HTTP 调用主路径
- 统一改成 Spring AI `ChatClient`

### 第三步：替换 `tool` 层

本步目标：

- 保留工具能力
- 删除自定义工具协议与注册表
- 改成 Spring AI Tool Calling

### 第四步：替换 `memory` 层

本步目标：

- 用 Spring AI `ChatMemory` 替换当前自定义记忆底层

### 第五步：让 `Executor` 接入 Spring AI

本步目标：

- `Executor` 通过 Spring AI 驱动工具执行、模型调用与上下文增强

### 第六步：清理旧抽象

删除旧实现：

- `llm.*`
- 旧 `ToolRegistry`
- 旧 `Tool` / `ToolRequest`
- 已废弃的旧 memory 路径

## 每一步的验证原则

迁移过程中，每一步都必须做到：

1. 先补设计文档
2. 再补测试
3. 再改代码
4. 最后跑完整验证

标准命令：

```bash
mvn -s .mvn/local-settings.xml test
```

## 风险点

Spring AI 迁移的主要风险包括：

- Spring Boot 与 Spring AI 版本兼容
- 配置项差异导致模型无法注入
- 旧 `tool` / `memory` 抽象与新基础设施并存期间产生双轨逻辑
- `Executor` 改造时造成主循环断裂

因此迁移必须按顺序推进，不能一次性全部替换。

## 结论

本次 Spring AI 迁移的核心原则是：

- **保留项目自己的 Agent 架构**
- **删除已经重复的 AI 基础设施实现**
- **让 Spring AI 成为模型、记忆、工具和调用链的统一底座**
