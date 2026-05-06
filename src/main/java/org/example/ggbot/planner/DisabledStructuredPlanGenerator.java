package org.example.ggbot.planner;

import java.util.Optional;

public class DisabledStructuredPlanGenerator implements StructuredPlanGenerator {

    @Override
    public Optional<String> generate(PlannerContext context) {
        return Optional.empty();
    }
}
