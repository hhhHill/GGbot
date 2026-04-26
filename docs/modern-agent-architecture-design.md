# 循环式 Agent 架构重构记录

## 重构目标

将现有“静态规则驱动的一次性 Plan 生成器”升级为“带状态、带反馈、可反思、可重新规划”的现代 Agent 架构。

目标执行模型如下：

```text
while (!done) {
    plan = planner.plan(state)
    result = executor.execute(plan)
    reflection = reflector.analyze(result, state)
    state = state.update(result, reflection)
    if (reflection.needReplan) {
        plan = rePlanner.replan(state, reflection)
    }
}
```

## 现有问题

当前主链路是：

```text
AgentRequest -> PlannerService -> Plan -> Tool 执行 -> AgentResult
```

它的问题在于：

- 只有一次性规划，没有持续状态
- 不记录每一步执行反馈
- 没有执行后的反思
- 一旦执行失败，无法根据失败结果自动调整计划
- 无法自然接入 LLM 作为 Planner / Reflector / RePlanner

## 新架构分层

### 1. `AgentState`

作为循环式 Agent 的核心状态对象。

至少包含：

- `userInput`
- `currentPlan`
- `executionHistory`
- `intermediateResults`
- `memory`
- `iteration`
- `done`
- `finalReply`

它负责承接“当前到了哪一步、拿到了什么中间结果、下一轮应该如何继续”。

### 2. `Planner`

负责根据当前 `AgentState` 生成新的 `Plan`，或返回当前仍可继续执行的计划。

当前仍保留规则式实现，后续可新增 LLM Planner。

### 3. `Executor`

负责执行计划中的待执行步骤，并把执行结果写回 Step 状态。

输出为 `ExecutionResult`，包括：

- 本轮执行了哪些步骤
- 每一步是否成功
- 是否存在失败
- 执行摘要

### 4. `Reflector`

负责在每轮执行后分析结果。

核心判断：

- 本轮是否成功
- 是否需要重试
- 是否需要重新规划
- 是否可以结束

这一层是后续对接 ReAct / Reflexion 风格推理的关键位置。

### 5. `RePlanner`

在执行失败或结果不满足条件时，对当前计划做调整。

当前阶段先提供规则化策略：

- 某步骤失败时，追加兜底总结步骤
- 保留执行历史
- 让 Agent 不至于一次失败后直接终止

后续可扩展为基于 LLM 的 re-plan。

### 6. `AgentRunner`

真正承载循环控制逻辑，按“规划 -> 执行 -> 反思 -> 更新状态 -> 必要时重新规划”的顺序运行。

### 7. `AgentService`

不再直接自己执行步骤，而是：

1. 创建初始 `AgentState`
2. 调用 `AgentRunner`
3. 更新任务状态与会话记忆
4. 组装外部返回的 `AgentResult`

## Plan 结构升级

### `Plan`

从一次性不可变步骤列表，升级为可动态修改的计划容器。

能力要求：

- 支持追加步骤
- 支持查询 pending steps
- 支持判断是否全部完成
- 支持在 re-plan 时增量扩展

### `PlanStep`

每个步骤都需要包含：

- `stepId`
- `toolName`
- `description`
- `instruction`
- `status`
- `result`
- `errorMessage`

## 关键扩展点

### 保留规则体系

当前 `planningRules` 仍然保留，但只作为 `Planner` 的一种实现方式。

未来可以继续增加：

- `LlmPlanner`
- `HybridPlanner`

### Reflection 可扩展

当前先用规则型 `Reflector`，后续可以扩展为：

- 失败原因分类
- 工具结果质量评分
- 自我修正建议生成

本轮增强后，`Reflector` 不再只判断“是否失败”，而是会区分失败类型：

- `TOOL_EXECUTION_FAILURE`
  工具直接执行失败，例如异常、参数错误、外部接口错误
- `EMPTY_OR_WEAK_RESULT`
  工具没有报错，但结果为空、过弱或不足以支持继续完成任务
- `PLAN_EXHAUSTED_BUT_NOT_DONE`
  当前计划已经没有待执行步骤，但状态还没有满足完成条件

这样 `ReflectionAnalysis` 会从简单布尔判断，升级为“状态 + 失败类型 + 建议策略”三层信息。

### RePlanning 可扩展

当前先实现简单兜底规则，后续可升级为：

- 基于历史执行记录重排步骤
- 根据工具失败类型替换工具
- 基于 LLM 重新生成增量计划

本轮增强后，`RePlanner` 会采用“策略化重规划”：

- 工具执行失败 -> 优先有限重试，超过上限后降级
- 结果过弱 -> 追加修正/补强步骤
- 计划耗尽但未完成 -> 生成增量步骤，而不是直接结束

为了避免 `RePlanner` 再次变成新的 God Class，
重规划将拆成：

- `ReplanStrategy`
- `ToolFailureReplanStrategy`
- `WeakResultReplanStrategy`
- `PlanExhaustedReplanStrategy`

由策略选择器根据 `ReflectionType` 决定采用哪一套重规划动作。

## 本次实现范围

本轮会完成：

- `AgentState`
- `Planner` / `Executor` / `Reflector` / `RePlanner` 接口
- `AgentRunner`
- 升级 `Plan` / `PlanStep`
- 让 `AgentService` 改为基于循环执行
- 保留现有规则 Planner 作为默认实现
- 增强 `Reflector` 的失败分类能力
- 将 `RePlanner` 升级为基于失败类型的策略化重规划

本轮不会完成：

- 真正的 LLM Planner / Reflector
- 多 Agent 协作
- 持久化状态机
- 异步任务编排

## 预期收益

完成后，系统将具备以下能力：

- 有状态执行
- 可记录中间结果
- 支持执行后反思
- 支持失败后重规划
- 能平滑接入 LLM 决策
- 能向更复杂的现代 Agent 架构继续演进
