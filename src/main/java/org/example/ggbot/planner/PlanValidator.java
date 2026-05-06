package org.example.ggbot.planner;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PlanValidator {

    public ValidationResult validate(Plan plan) {
        List<String> errors = new ArrayList<>();
        if (plan == null) {
            return ValidationResult.invalid(List.of("Plan must not be null."));
        }
        if (plan.getGoal() == null || plan.getGoal().isBlank()) {
            errors.add("Plan goal must not be blank.");
        }
        if (plan.getSteps().isEmpty()) {
            errors.add("Plan steps must not be empty.");
        }
        if (plan.getSteps().size() < 1 || plan.getSteps().size() > 5) {
            errors.add("Plan steps size must be between 1 and 5.");
        }

        Map<String, PlanStep> stepById = new HashMap<>();
        for (PlanStep step : plan.getSteps()) {
            if (step.getStepId() == null || step.getStepId().isBlank()) {
                errors.add("Step id must not be blank.");
                continue;
            }
            if (stepById.put(step.getStepId(), step) != null) {
                errors.add("Duplicate step id: " + step.getStepId());
            }
            if (step.getType() == null) {
                errors.add("Step type must not be null for step " + step.getStepId());
            }
            if (step.getDescription() == null || step.getDescription().isBlank()) {
                errors.add("Step description must not be blank for step " + step.getStepId());
            }
        }

        for (PlanStep step : plan.getSteps()) {
            for (String dependency : step.getDependsOn()) {
                if (!stepById.containsKey(dependency)) {
                    errors.add("Step %s depends on missing step %s".formatted(step.getStepId(), dependency));
                }
            }
        }

        if (hasCycle(plan.getSteps())) {
            errors.add("Plan steps must not contain circular dependencies.");
        }

        if (plan.isNeedClarification()) {
            PlanStep firstStep = plan.getSteps().isEmpty() ? null : plan.getSteps().get(0);
            if (firstStep == null || firstStep.getType() != StepType.CLARIFY) {
                errors.add("Clarification plan must start with CLARIFY.");
            }
        }

        if (plan.getDeliverableType() == DeliverableType.PPT && containsNoStep(plan, StepType.GENERATE_PPT)) {
            errors.add("PPT deliverable should contain GENERATE_PPT step.");
        }
        if (plan.getDeliverableType() == DeliverableType.DOC && containsNoStep(plan, StepType.GENERATE_DOC)) {
            errors.add("DOC deliverable should contain GENERATE_DOC step.");
        }
        if (plan.getDeliverableType() == DeliverableType.SUMMARY && containsNoStep(plan, StepType.SUMMARIZE)) {
            errors.add("SUMMARY deliverable should contain SUMMARIZE step.");
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    private boolean containsNoStep(Plan plan, StepType expectedType) {
        return plan.getSteps().stream().noneMatch(step -> step.getType() == expectedType);
    }

    private boolean hasCycle(List<PlanStep> steps) {
        Map<String, List<String>> graph = new HashMap<>();
        for (PlanStep step : steps) {
            graph.put(step.getStepId(), step.getDependsOn());
        }

        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String stepId : graph.keySet()) {
            if (dfsCycle(stepId, graph, visiting, visited)) {
                return true;
            }
        }
        return false;
    }

    private boolean dfsCycle(String stepId, Map<String, List<String>> graph, Set<String> visiting, Set<String> visited) {
        if (visited.contains(stepId)) {
            return false;
        }
        if (!visiting.add(stepId)) {
            return true;
        }
        for (String next : graph.getOrDefault(stepId, List.of())) {
            if (dfsCycle(next, graph, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(stepId);
        visited.add(stepId);
        return false;
    }
}
