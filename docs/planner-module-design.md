# Planner 模块设计记录

## 设计目标

本次重构的目标不是单纯增强关键词命中率，而是把 `planner` 模块整理成一个清晰、可扩展的规划框架，为后续接入 LLM 规划能力预留稳定边界。

当前阶段希望解决的问题：

- 避免把所有规则写死在 `PlannerService` 的 `if-else` 中
- 将“规则命中”和“步骤生成”拆开
- 让后续新增规则时尽量只增加类，而不是持续膨胀中心服务
- 为后续 `LlmPlanner` 接入保留扩展点

## 当前问题

当前 `PlannerService` 直接承担了三类职责：

1. 关键词规则判断
2. 意图归并
3. 执行步骤生成

这样会带来几个问题：

- 规则增加后，`PlannerService` 会持续膨胀
- 意图识别与步骤生成耦合在一起，不利于后续替换任一部分
- 无法自然引入“多个规则共同贡献一个最终意图”的中间层
- 后续接入 LLM 时，只能把 LLM 调用硬塞进现有服务中

## 重构方案

本次将 `planner` 模块拆成以下几个核心角色：

### 1. `PlanningRule`

规则接口，负责判断当前输入是否命中，并输出一部分规划信号。

职责：

- 定义当前规则关心的关键词
- 决定是否命中
- 产出 `PlanningSignal`

当前阶段会先实现：

- `DocPlanningRule`
- `PptPlanningRule`

### 2. `PlanningSignal`

规则层与规划层之间的中间模型。

它不直接等于最终 `IntentType`，而是表达“规划信号”，例如：

- 是否需要文档
- 是否需要 PPT
- 命中的关键词
- 规则来源

这样多个规则就可以共同构造最终规划，而不是每个规则直接决定最终意图。

### 3. `PlanStepFactory`

负责把“最终规划信号”转换成 `PlanStep` 列表。

这样可以把“识别用户要做什么”和“如何组织执行步骤”拆开。

当前规则：

- `needDoc = true` -> 生成 `GENERATE_DOC`
- `needPpt = true` -> 生成 `GENERATE_PPT`
- 两者都不需要 -> 生成 `SUMMARIZE`

### 4. `PlannerService`

作为 `planner` 模块总入口，只负责：

1. 收集所有 `PlanningRule`
2. 汇总成一个最终 `PlanningSignal`
3. 计算 `IntentType`
4. 调用 `PlanStepFactory` 生成 `Plan`

`PlannerService` 不再直接关心具体关键词。

## 数据流

```text
AgentRequest
   |
   v
PlanningRule[]
   |
   v
PlanningSignal
   |
   +--> IntentTypeResolver（由 PlannerService 内部完成）
   |
   v
PlanStepFactory
   |
   v
Plan
```

## 与 LLM 的扩展关系

后续如果要接入大模型规划能力，建议增加：

- `LlmPlanningService` 或 `LlmPlanner`

它的输出应该保持与规则规划一致，至少输出：

- `PlanningSignal`
  或者
- 直接输出 `Plan`

推荐方式：

1. 先保留规则规划作为基础能力
2. LLM 规划作为增强能力按配置启用
3. 当 LLM 不可用、超时或结果非法时，自动降级到规则规划

这样 `planner` 层会保留稳定的调用方式：

- `PlannerService.plan(request)`

而不会把上层调用方绑死在某一种规划实现上。

## 本次落地范围

本轮只做结构性重构，不做复杂规则增强。

包含：

- `PlanningRule` 抽象
- `PlanningSignal` 中间模型
- `PlanStepFactory`
- `DocPlanningRule`
- `PptPlanningRule`
- 重构 `PlannerService`
- 补充对应单元测试

不包含：

- LLM 规划实现
- 复杂优先级系统
- 规则权重模型
- 多轮上下文感知规划
- 规则配置化

## 预期收益

完成后，`planner` 模块会具备这些特点：

- 结构边界清晰
- 规则新增成本低
- 测试更容易编写
- 更适合后续引入 LLM 规划器
- `AgentService` 无需感知规划实现细节变化
