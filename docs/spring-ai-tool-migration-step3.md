# Spring AI 迁移实施记录 - 第 3 步

## 目标

将项目的 `tool` 层从“自定义工具协议 + 注册表”迁移为“Spring AI 可注册工具对象 + 项目级执行适配层”。

这一阶段的重点是：

- 删除 `Tool`、`ToolRegistry`、`ToolRequest` 这类重复抽象
- 保留 `ToolName` 和 `tool.model.*`，避免影响现有计划与反思机制
- 让具体工具能力具备 Spring AI `@Tool` 注册能力
- 让 `DefaultExecutor` 不再依赖自定义工具注册表

## 设计

### 保留的部分

- `ToolName`
- `ToolResult`
- `tool.model.*`
- `tool.impl.*` 的业务能力

原因：

- `ToolName` 仍然是规划层与执行层之间的稳定协议
- `ToolResult` 当前还承担 Agent 侧的观察结果载体职责
- `tool.model.*` 是领域产物模型，不属于 Spring AI 基础设施

### 删除的部分

- `Tool`
- `ToolRegistry`
- `ToolRequest`

原因：

- 它们本质上是在重复定义“工具是什么、怎么注册、怎么执行”
- 这些职责更适合交给 Spring Bean 装配和 Spring AI Tool 元数据

### 新结构

- 每个具体工具类保留为 Spring Bean
- 每个工具类提供：
  - 一个 `@Tool` 标注的方法，供后续 `ChatClient.tools(...)` 或 `ToolCallbackProvider` 使用
  - 一个项目内部调用方法，供 `SpringAiToolExecutor` 直接执行
- 新增 `SpringAiToolExecutor`
  - 接收 `ToolName`
  - 分发到对应工具 Bean
  - 统一返回 `ToolResult`
- 新增 `ToolCallbackProvider` Bean
  - 通过 Spring AI `MethodToolCallbackProvider` 暴露当前工具对象

## 当前边界

这一阶段完成后：

- 工具能力已经具备 Spring AI 接入基础
- `Executor` 已不再依赖自定义注册表
- 但工具还未由模型自主调用

## 下一步

下一步应该进入：

- Spring AI 迁移第 4 步：替换 `memory` 层

目标是：

- 用 Spring AI `ChatMemory` 承接会话记忆
- 让后续 `ChatClient` 可以通过 advisor 直接使用记忆
