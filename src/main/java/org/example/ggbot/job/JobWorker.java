package org.example.ggbot.job;

import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentResult;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.ai.ChatFallbackPolicy;
import org.example.ggbot.common.JsonUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobWorker {

    private static final String RUNNING_MESSAGE = "正在调用模型";
    private static final String FALLBACK_REPLY = ChatFallbackPolicy.createDefault().fallbackReply();

    private final AgentService agentService;
    private final JobService jobService;
    private final JsonUtils jsonUtils;

    public void process(String jobId, AgentRequest request) {
        jobService.markRunning(jobId, RUNNING_MESSAGE);
        try {
            AgentResult result = agentService.handle(request);
            jobService.markSucceeded(jobId, jsonUtils.toJson(result));
        } catch (RuntimeException ex) {
            jobService.markFailed(jobId, errorMessageOf(ex), FALLBACK_REPLY);
        }
    }

    private String errorMessageOf(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}
