package org.example.ggbot.planner;

import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ggbot.agent.AgentState;
import org.springframework.stereotype.Service;

/**
 * `planner` 模块的总入口。
 *
 * <p>它自身不直接编写具体关键词规则，而是负责：
 *
 * <ol>
 *   <li>调用所有 `PlanningRule` 收集命中结果</li>
 *   <li>把这些结果合并成一个最终 `PlanningSignal`</li>
 *   <li>根据规划信号计算 `IntentType`</li>
 *   <li>调用 `PlanStepFactory` 生成最终 `Plan`</li>
 * </ol>
 *
 * <p>这样后续无论新增规则还是接入 LLM 规划器，都不需要重写上层调用方式。
 */
@Slf4j
@Service
@Data
@RequiredArgsConstructor
public class PlannerService implements Planner {

    private final StructuredPlanGenerator structuredPlanGenerator;
    private final StructuredPlanParser structuredPlanParser;
    private final PlanValidator planValidator;
    private final List<PlanningRule> planningRules;
    private final PlanStepFactory planStepFactory;

    /**
     * 根据当前 `AgentState` 生成计划。
     */
    @Override
    public Plan plan(AgentState state) {
        if (state.getCurrentPlan() != null && state.getCurrentPlan().hasPendingSteps()) {
            return state.getCurrentPlan();
        }

        PlannerContext context = new PlannerContext(
                state.getUserInput(),
                state.getMemory().stream().collect(Collectors.joining("\n"))
        );
        log.info("Planner start. structuredPlannerEnabled={}", !(structuredPlanGenerator instanceof DisabledStructuredPlanGenerator));
        Plan structuredPlan = tryStructuredPlan(context);
        if (structuredPlan != null) {
            return structuredPlan;
        }

        log.info("Planner falling back to rule-based planning.");
        return buildFallbackPlan(state);
    }

    private Plan tryStructuredPlan(PlannerContext context) {
        try {
            return structuredPlanGenerator.generate(context)
                    .flatMap(rawPlan -> {
                        log.info("Structured planner returned content, parsing JSON.");
                        return structuredPlanParser.parse(rawPlan);
                    })
                    .flatMap(plan -> {
                        ValidationResult validation = planValidator.validate(plan);
                        if (!validation.isValid()) {
                            log.warn("Structured plan validation failed: {}", validation.errors());
                            return java.util.Optional.empty();
                        }
                        log.info("Structured plan validation passed.");
                        return java.util.Optional.of(plan);
                    })
                    .orElse(null);
        } catch (RuntimeException ex) {
            log.warn("Structured planner invocation failed.", ex);
            return null;
        }
    }

    private Plan buildFallbackPlan(AgentState state) {
        String input = state.getUserInput() == null ? "" : state.getUserInput();
        PlanningSignal signal = planningRules.stream()
                .map(rule -> rule.evaluate(state))
                .reduce(PlanningSignal.empty(), (left, right) -> {
                    left.merge(right);
                    return left;
                });

        enrichFallbackSignal(signal, input);
        IntentType intentType = resolveIntentType(signal);
        List<PlanStep> steps = planStepFactory.createSteps(signal, input);
        Plan plan = new Plan();
        plan.setIntentType(intentType);
        plan.setGoal(input);
        plan.setDeliverableType(resolveDeliverableType(signal));
        plan.setNeedClarification(signal.isNeedClarification());
        plan.setMultiStep(steps.size() > 1);
        plan.setNeedDoc(signal.isNeedDoc());
        plan.setNeedPpt(signal.isNeedPpt());
        plan.appendSteps(steps);
        plan.syncLegacyFlags();
        return plan;
    }

    private void enrichFallbackSignal(PlanningSignal signal, String input) {
        String normalized = input == null ? "" : input.trim();
        boolean genericBriefing = normalized.contains("汇报")
                && !normalized.contains("PPT")
                && !normalized.contains("ppt")
                && !normalized.contains("文档")
                && normalized.length() <= 10;
        if (genericBriefing) {
            signal.setNeedClarification(true);
            signal.setClarificationQuestion("请补充汇报主题、汇报对象，以及希望输出文档还是 PPT。");
            return;
        }

        if (signal.isNeedPpt() && !signal.isNeedDoc()) {
            signal.setNeedDoc(true);
        }
    }

    private DeliverableType resolveDeliverableType(PlanningSignal signal) {
        if (signal.isNeedClarification()) {
            return DeliverableType.UNKNOWN;
        }
        if (signal.isNeedDoc() && signal.isNeedPpt()) {
            return DeliverableType.MIXED;
        }
        if (signal.isNeedDoc()) {
            return DeliverableType.DOC;
        }
        if (signal.isNeedPpt()) {
            return DeliverableType.PPT;
        }
        return DeliverableType.SUMMARY;
    }

    /**
     * 根据合并后的规划信号计算最终意图。
     */
    private IntentType resolveIntentType(PlanningSignal signal) {
        if (signal.isNeedClarification()) {
            return IntentType.CHAT;
        }
        if (signal.isNeedDoc() && signal.isNeedPpt()) {
            return IntentType.CREATE_DOC_AND_PPT;
        }
        if (signal.isNeedDoc()) {
            return IntentType.CREATE_DOC;
        }
        if (signal.isNeedPpt()) {
            return IntentType.CREATE_PPT;
        }
        return IntentType.CHAT;
    }
}
