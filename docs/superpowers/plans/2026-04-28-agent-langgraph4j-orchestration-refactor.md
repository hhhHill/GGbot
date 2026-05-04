# Agent LangGraph4j Orchestration Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the custom Agent orchestration loop with a LangGraph4j state graph and keep the codebase LangGraph-only.

**Architecture:** A thin `AgentRunner` facade delegates directly to `LangGraphAgentRunner`. LangGraph4j owns the flow, while `AgentNodes` keeps the business logic inside the existing planner, executor, reflector, and replanner beans.

**Tech Stack:** Java 17, Spring Boot 3.4.13, Spring AI 1.0.0, LangGraph4j 1.8.12, Maven, JUnit 5, Mockito, AssertJ.

---

## 文件映射

**新增：**

- `src/main/java/org/example/ggbot/agent/graph/AgentGraphProperties.java`
- `src/main/java/org/example/ggbot/agent/graph/AgentRoutingDecision.java`
- `src/main/java/org/example/ggbot/agent/graph/AgentGraphRouter.java`
- `src/main/java/org/example/ggbot/agent/graph/GGBotAgentGraphState.java`
- `src/main/java/org/example/ggbot/agent/graph/AgentNodes.java`
- `src/main/java/org/example/ggbot/agent/graph/AgentGraphFactory.java`
- `src/main/java/org/example/ggbot/agent/runner/LangGraphAgentRunner.java`
- `src/test/java/org/example/ggbot/agent/LangGraphAgentRunnerTest.java`
- `src/test/java/org/example/ggbot/agent/graph/AgentNodesTest.java`
- `src/test/java/org/example/ggbot/config/AgentGraphPropertiesTest.java`

**修改：**

- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/java/org/example/ggbot/config/AppConfig.java`
- `src/main/java/org/example/ggbot/agent/AgentRunner.java`
- `src/test/java/org/example/ggbot/agent/AgentRunnerTest.java`
- `src/test/java/org/example/ggbot/agent/AgentServiceTest.java`

## 任务 1：引入 LangGraph4j 依赖和运行配置

**目标：**

- 引入 `langgraph4j-bom` 和 `langgraph4j-core`
- 开启配置绑定和校验
- 新增 `AgentGraphProperties`

**验证：**

- `AgentGraphProperties.maxIterations` 默认值为 `10`
- `maxIterations < 1` 直接在绑定阶段失败

## 任务 2：把 AgentRunner 收敛为 LangGraph 门面

**目标：**

- `AgentRunner` 只委托 `LangGraphAgentRunner`
- 删除 legacy 分支、legacy runner 和 mode 切换

**验证：**

- `AgentRunnerTest` 直接验证门面委托 LangGraph runner
- `AgentService` 对外接口不变

## 任务 3：新增图状态与节点包装

**目标：**

- `GGBotAgentGraphState` 包装现有 `AgentState`
- `AgentNodes` 只做状态读取、业务调用、结果回写
- `AgentGraphRouter` 负责条件分支

**验证：**

- `AgentNodesTest` 覆盖 `plan -> execute -> reflect` 的写回行为

## 任务 4：构建 LangGraph4j Runner

**目标：**

- `AgentGraphFactory` 构建并缓存 `StateGraph`
- `LangGraphAgentRunner` 调用 `invoke(...)` 得到最终 `AgentState`

**验证：**

- `LangGraphAgentRunnerTest` 覆盖完整图执行
- 最大循环次数到达时写入默认终止回复

## 任务 5：补齐兼容性与异常路径测试

**目标：**

- 保留对 `AgentService`、`ReflectionAndReplan`、图节点和 runner 的回归测试
- 删除 legacy 专属测试

**验证：**

- 全量测试通过

## 结论

该计划的最终落地结果是 LangGraph-only：

- 没有 `LegacyAgentRunner`
- 没有 `agent.runner.mode`
- 没有双路径切换
- 所有编排都通过 LangGraph4j 完成
