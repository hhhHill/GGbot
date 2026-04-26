# Spring AI 迁移实施记录 - 第 1 步

## 目标

先把项目接入 Spring AI 的基础设施层，但暂时不改 Agent 主链路。

这一阶段只做：

- 升级 Spring Boot 到 Spring AI 官方支持的版本范围
- 引入 Spring AI BOM 与模型 starter
- 注入基础 `ChatClient` / `ChatMemory` Bean
- 保持现有 `agent`、`planner`、`tool`、`memory`、`llm` 逻辑不被破坏

## 官方依据

本次实现依据 Spring AI 官方文档中的两个约束：

1. Spring AI 官方 Getting Started 文档说明：Spring AI 支持 Spring Boot `3.4.x` 和 `3.5.x`
2. Spring AI 官方推荐通过 `spring-ai-bom` 管理依赖版本，并使用 `spring-ai-starter-model-openai` 这类 starter 引入模型支持

## 本次改动

### 1. 升级 Spring Boot

将项目从：

- `Spring Boot 3.3.5`

升级到：

- `Spring Boot 3.4.13`

原因：

- 避免使用 Spring AI 时出现自动配置兼容性问题

### 2. 引入 Spring AI BOM

在 `pom.xml` 中增加：

- `org.springframework.ai:spring-ai-bom`

这样后续接入更多 Spring AI 模块时，依赖版本由 BOM 统一管理。

### 3. 引入 OpenAI 模型 Starter

增加：

- `org.springframework.ai:spring-ai-starter-model-openai`

原因：

- 先接入最通用的 `ChatModel` / `ChatClient` 基础设施
- 当前项目原本也是围绕 OpenAI-compatible 风格设计的，迁移成本最低

### 4. 新增 `SpringAiConfig`

新增基础配置类，提供：

- `ChatMemory`
- `ChatClient`

其中 `ChatClient` 使用 `@ConditionalOnBean(ChatModel.class)` 保护，
避免在没有启用模型配置时影响当前测试与启动。

### 5. 更新配置文件

在 `application.yml` 中新增 `spring.ai` 配置入口，但默认：

- `spring.ai.model.chat=none`

这表示：

- 当前项目默认不会启用真实模型
- 仍然可以保持现有测试通过
- 后续只要打开配置并提供 key/base-url/model，即可切换到 Spring AI 模型接入

同时显式排除了当前阶段不会使用的 OpenAI 自动配置：

- Audio Speech
- Audio Transcription
- Embedding
- Image
- Moderation

原因：

- `spring-ai-starter-model-openai` 会同时带入这些自动配置
- 在未配置真实 API key 的情况下，它们会在应用启动时直接失败
- 当前阶段我们只需要 Chat 基础设施接入，不需要这些能力

## 本步刻意不做的事

本步不做以下改动：

- 不删除 `llm.*`
- 不删除 `tool.ToolRegistry`
- 不替换 `memory.*`
- 不修改 `AgentRunner` / `Executor` 主链路

原因：

- 这是基础设施接入阶段，目标是“先接上 Spring AI”，不是“第一步就推翻旧主链路”

## 下一步

下一步应该进入：

- Spring AI 迁移第 2 步：替换 `llm` 层

目标是：

- 让项目的模型调用统一走 `ChatClient`
- 停用当前自定义 `OpenAiCompatibleLlmClient`
