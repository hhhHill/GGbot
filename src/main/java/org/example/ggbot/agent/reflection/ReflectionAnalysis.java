package org.example.ggbot.agent.reflection;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ReflectionAnalysis {

    private final boolean success;
    private final boolean needRetry;
    private final boolean needReplan;
    private final boolean done;
    private final ReflectionType reflectionType;
    private final String thought;
    private final String observation;
    private final String recommendedAction;
}
