package org.example.ggbot.planner;

import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Plan {

    private final IntentType intentType;
    private final boolean needDoc;
    private final boolean needPpt;
    private final List<PlanStep> steps;
}
