package org.example.ggbot.job;

import java.util.Locale;
import java.util.Set;
import org.example.ggbot.agent.AgentRequest;
import org.springframework.stereotype.Component;

@Component
public class AsyncExecutionDecider {

    private static final Set<String> ASYNC_KEYWORDS = Set.of(
            "导出",
            "export",
            "文档",
            "document",
            "ppt",
            "汇报"
    );

    public AsyncExecutionMode decide(AgentRequest request) {
        String userInput = request.getUserInput();
        if (userInput == null || userInput.isBlank()) {
            return AsyncExecutionMode.SYNC;
        }

        String normalizedInput = userInput.toLowerCase(Locale.ROOT);
        return ASYNC_KEYWORDS.stream().anyMatch(normalizedInput::contains)
                ? AsyncExecutionMode.ASYNC
                : AsyncExecutionMode.SYNC;
    }
}
