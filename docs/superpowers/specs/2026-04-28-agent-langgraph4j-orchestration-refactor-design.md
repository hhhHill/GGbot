# Agent LangGraph4j Orchestration Refactor Design

## 背景

当前仓库的 Agent 编排层已经改造成 LangGraph4j 驱动。最终代码只保留一条执行链路：

- `AgentService -> AgentRunner -> LangGraphAgentRunner -> StateGraph -> AgentNodes -> Planner / Executor / Reflector / RePlanner`

`Planner`、`Executor`、`Reflector`、`RePlanner` 继续保留原有业务逻辑，只替换编排层。

## 当前实现分析

`AgentRunner` 现在只是一个稳定门面，内部直接委托 `LangGraphAgentRunner`。  
`LangGraphAgentRunner` 使用 [`AgentGraphFactory`](D:/GGbot/src/main/java/org/example/ggbot/agent/graph/AgentGraphFactory.java) 构建并缓存 LangGraph4j 图。  
节点包装集中在 [`AgentNodes`](D:/GGbot/src/main/java/org/example/ggbot/agent/graph/AgentNodes.java)，状态适配层集中在 [`GGBotAgentGraphState`](D:/GGbot/src/main/java/org/example/ggbot/agent/graph/GGBotAgentGraphState.java)。

`AgentState` 仍然是业务状态本体，图状态只负责把它包装进 LangGraph4j 的可执行状态里。  
当前循环语义仍然保持原样：

- `plan` 前递增一次 `iteration`
- `execute` 只执行当前 `Plan` 的 pending step
- `reflect` 负责更新 `AgentState`
- `replan` 根据 `ReflectionAnalysis` 更新 `currentPlan`
- 最大轮次到达后写入 `"Agent reached the maximum iteration limit."`

## 重构边界

不应该改：

- `Planner` / `PlannerService`
- `Executor` / `DefaultExecutor`
- `Reflector` / `SimpleReflector`
- `RePlanner` / `DefaultRePlanner`
- 工具执行、任务管理、会话记忆、最终回复累积语义

可以新增：

- `org.example.ggbot.agent.graph.AgentGraphProperties`
- `org.example.ggbot.agent.graph.AgentRoutingDecision`
- `org.example.ggbot.agent.graph.AgentGraphRouter`
- `org.example.ggbot.agent.graph.GGBotAgentGraphState`
- `org.example.ggbot.agent.graph.AgentNodes`
- `org.example.ggbot.agent.graph.AgentGraphFactory`
- `org.example.ggbot.agent.runner.LangGraphAgentRunner`

需要最小修改：

- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/java/org/example/ggbot/config/AppConfig.java`
- `src/main/java/org/example/ggbot/agent/AgentRunner.java`
- 相关测试

## 依赖确认

当前仓库使用：

- Java 17
- Spring Boot 3.4.13
- Spring AI 1.0.0

已引入并验证：

- `org.bsc.langgraph4j:langgraph4j-bom:1.8.12`
- `org.bsc.langgraph4j:langgraph4j-core:1.8.12`
- `org.springframework.boot:spring-boot-starter-validation`

这套依赖组合在当前仓库和 Java 17 环境下可用。

## 目标代码结构

- `org.example.ggbot.agent.AgentRunner`
- `org.example.ggbot.agent.runner.LangGraphAgentRunner`
- `org.example.ggbot.agent.graph.AgentGraphProperties`
- `org.example.ggbot.agent.graph.AgentRoutingDecision`
- `org.example.ggbot.agent.graph.AgentGraphRouter`
- `org.example.ggbot.agent.graph.GGBotAgentGraphState`
- `org.example.ggbot.agent.graph.AgentNodes`
- `org.example.ggbot.agent.graph.AgentGraphFactory`

## Graph 流程

1. `plan`
2. `execute`
3. `reflect`
4. 条件分支：
   - 完成 -> `END`
   - 需要重规划 -> `replan`
   - 继续执行 -> `execute`
   - 达到最大循环次数 -> `END`
5. `replan -> execute`

## 运行配置

当前只保留图编排所需参数：

```yaml
agent:
  runner:
    max-iterations: 10
```

`max-iterations` 用于限制图的最大循环次数。

## 风险

- LangGraph4j API 变动风险
- 状态对象可变性风险
- 循环终止条件风险
- 异步与上下文传播风险
- Spring Bean 生命周期风险

## 结论

这次改造的最终状态是 LangGraph-only：

- 不再保留 legacy runner
- 不再保留 `agent.runner.mode`
- 不再保留双路径切换
- 所有编排逻辑统一收敛到 LangGraph4j
