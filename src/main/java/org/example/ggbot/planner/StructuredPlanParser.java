package org.example.ggbot.planner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StructuredPlanParser {

    private final ObjectMapper objectMapper;

    public StructuredPlanParser() {
        this(new ObjectMapper());
    }

    public StructuredPlanParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Optional<Plan> parse(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            StructuredPlanPayload payload = objectMapper.readValue(json, StructuredPlanPayload.class);
            return Optional.of(toPlan(payload));
        } catch (IOException | IllegalArgumentException ex) {
            log.warn("Failed to parse structured plan JSON.", ex);
            return Optional.empty();
        }
    }

    private Plan toPlan(StructuredPlanPayload payload) {
        Plan plan = new Plan();
        plan.setGoal(payload.goal);
        plan.setDeliverableType(payload.deliverableType == null
                ? DeliverableType.UNKNOWN
                : DeliverableType.valueOf(payload.deliverableType));
        plan.setNeedClarification(Boolean.TRUE.equals(payload.needClarification));
        plan.setMultiStep(Boolean.TRUE.equals(payload.multiStep));

        List<PlanStep> steps = new ArrayList<>();
        if (payload.steps != null) {
            for (StructuredPlanStepPayload stepPayload : payload.steps) {
                StepType stepType = StepType.valueOf(stepPayload.type);
                steps.add(new PlanStep(
                        stepPayload.id,
                        stepType,
                        stepType.defaultToolName(),
                        stepPayload.desc,
                        stepPayload.desc,
                        safeList(stepPayload.dependsOn),
                        safeList(stepPayload.inputRefs),
                        stepPayload.expectedOutput
                ));
            }
        }
        plan.appendSteps(steps);
        if (plan.getSteps().size() > 1) {
            plan.setMultiStep(true);
        }
        plan.syncLegacyFlags();
        return plan;
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StructuredPlanPayload {
        public String goal;
        public String deliverableType;
        public Boolean needClarification;
        public Boolean multiStep;
        public List<StructuredPlanStepPayload> steps;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StructuredPlanStepPayload {
        public String id;
        public String type;
        public String desc;
        public List<String> dependsOn;
        public List<String> inputRefs;
        public String expectedOutput;
    }
}
