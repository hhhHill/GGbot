package org.example.ggbot.agent.reflection;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.execution.ExecutionResult;
import org.example.ggbot.agent.execution.StepExecutionRecord;
import org.example.ggbot.tool.ToolResult;
import org.springframework.stereotype.Component;

/**
 * 简单反思器实现
 * 基于规则判断执行结果的质量，不需要调用LLM
 */
@Component
@Data
@RequiredArgsConstructor
public class SimpleReflector implements Reflector {

    @Override
    public ReflectionAnalysis analyze(AgentState state, ExecutionResult result) {
        if (result.hasFailures()) {
            return new ReflectionAnalysis(
                    false,
                    false,
                    true,
                    false,
                    ReflectionType.TOOL_EXECUTION_FAILURE,
                    "本轮执行存在失败步骤",
                    result.getSummary(),
                    "replan"
            );
        }

        if (isWeakResult(result)) {
            return new ReflectionAnalysis(
                    false,
                    false,
                    true,
                    false,
                    ReflectionType.EMPTY_OR_WEAK_RESULT,
                    "本轮执行没有报错，但结果为空或质量过弱",
                    result.getSummary(),
                    "improve"
            );
        }

        if (state.getCurrentPlan() != null && state.getCurrentPlan().allStepsCompleted()) {
            return new ReflectionAnalysis(
                    true,
                    false,
                    false,
                    true,
                    ReflectionType.SUCCESS,
                    "所有步骤已完成",
                    result.getSummary(),
                    "finish"
            );
        }

        if ((state.getCurrentPlan() == null || !state.getCurrentPlan().hasPendingSteps()) && !state.isDone()) {
            return new ReflectionAnalysis(
                    false,
                    false,
                    true,
                    false,
                    ReflectionType.PLAN_EXHAUSTED_BUT_NOT_DONE,
                    "当前计划已经耗尽，但任务仍未结束",
                    result.getSummary(),
                    "extend-plan"
            );
        }

        if (state.getCurrentPlan() != null && state.getCurrentPlan().hasPendingSteps()) {
            return new ReflectionAnalysis(
                    true,
                    false,
                    false,
                    false,
                    ReflectionType.SUCCESS,
                    "当前步骤成功，但还有剩余步骤",
                    result.getSummary(),
                    "continue"
            );
        }

        return new ReflectionAnalysis(
                true,
                false,
                false,
                true,
                ReflectionType.SUCCESS,
                "所有步骤已完成",
                result.getSummary(),
                "finish"
        );
    }

    private boolean isWeakResult(ExecutionResult result) {
        if (result.getRecords().isEmpty()) {
            return false;
        }
        StepExecutionRecord latest = result.getRecords().get(result.getRecords().size() - 1);
        if (latest.getObservation() == null || latest.getObservation().isBlank()) {
            return true;
        }
        if (latest.getResult() instanceof ToolResult toolResult) {
            return (toolResult.getSummary() == null || toolResult.getSummary().isBlank()) && toolResult.getArtifact() == null;
        }
        return false;
    }
}
