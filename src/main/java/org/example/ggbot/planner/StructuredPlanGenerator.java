package org.example.ggbot.planner;

import java.util.Optional;

public interface StructuredPlanGenerator {

    Optional<String> generate(PlannerContext context);
}
