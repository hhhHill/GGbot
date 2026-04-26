package org.example.ggbot.adapter.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.WebChatAcceptedResponse;
import org.example.ggbot.adapter.web.dto.WebChatRequest;
import org.example.ggbot.adapter.web.dto.WebChatResponse;
import org.example.ggbot.adapter.web.dto.WebJobStatusResponse;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentResult;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.common.JsonUtils;
import org.example.ggbot.job.AsyncExecutionDecider;
import org.example.ggbot.job.AsyncExecutionMode;
import org.example.ggbot.job.JobRecord;
import org.example.ggbot.job.JobService;
import org.example.ggbot.job.JobStatus;
import org.example.ggbot.job.JobWorker;
import org.example.ggbot.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
@Data
@RequiredArgsConstructor
public class WebAgentController {

    private final AgentService agentService;
    private final AsyncExecutionDecider asyncExecutionDecider;
    private final JobService jobService;
    private final JobWorker jobWorker;
    private final Executor jobWorkerExecutor;
    private final JsonUtils jsonUtils;

    @PostMapping("/chat")
    public ApiResponse<?> chat(@RequestBody WebChatRequest request) {
        AgentRequest agentRequest = toAgentRequest(request);
        if (asyncExecutionDecider.decide(agentRequest) == AsyncExecutionMode.ASYNC) {
            JobRecord record = jobService.create(request.getConversationId(), request.getUserId(), jsonUtils.toJson(request));
            dispatch(record.getJobId(), agentRequest);
            return ApiResponse.success(new WebChatAcceptedResponse(true, record.getJobId(), record.getStatus().name()));
        }

        AgentResult agentResult = agentService.handle(agentRequest);

        return ApiResponse.success(new WebChatResponse(
                agentResult.getTaskId(),
                agentResult.getReplyText(),
                agentResult.getIntentType().name(),
                agentResult.getArtifactSummaries()
        ));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<WebJobStatusResponse> jobStatus(@PathVariable String jobId) {
        JobRecord record = jobService.get(jobId);
        Map<String, Object> result = resultMap(record);
        return ApiResponse.success(new WebJobStatusResponse(
                record.getJobId(),
                record.getStatus().name(),
                record.getProgressMessage(),
                record.getRetryCount(),
                canRetry(record),
                stringValue(result, "taskId"),
                stringValue(result, "intentType"),
                stringList(result, "artifactSummaries"),
                stringValue(result, "replyText"),
                record.getFallbackReply()
        ));
    }

    @PostMapping("/jobs/{jobId}/retry")
    public ApiResponse<WebChatAcceptedResponse> retry(@PathVariable String jobId) {
        JobRecord retried = jobService.retry(jobId);
        WebChatRequest request = jsonUtils.toMap(retried.getOriginalRequestPayload()).isEmpty()
                ? new WebChatRequest()
                : toWebChatRequest(jsonUtils.toMap(retried.getOriginalRequestPayload()));
        AgentRequest agentRequest = toAgentRequest(request);
        dispatch(retried.getJobId(), agentRequest);
        return ApiResponse.success(new WebChatAcceptedResponse(true, retried.getJobId(), retried.getStatus().name()));
    }

    private AgentRequest toAgentRequest(WebChatRequest request) {
        return new AgentRequest(
                request.getConversationId(),
                request.getUserId(),
                request.getMessage(),
                AgentChannel.WEB,
                null,
                request.getConversationId(),
                new HashMap<>()
        );
    }

    private WebChatRequest toWebChatRequest(Map<String, Object> payload) {
        WebChatRequest request = new WebChatRequest();
        request.setConversationId((String) payload.getOrDefault("conversationId", "web-mvp-session"));
        request.setUserId((String) payload.getOrDefault("userId", "demo-user"));
        request.setMessage((String) payload.getOrDefault("message", ""));
        return request;
    }

    private void dispatch(String jobId, AgentRequest request) {
        jobWorkerExecutor.execute(() -> jobWorker.process(jobId, request));
    }

    private boolean canRetry(JobRecord record) {
        return record.getStatus() == JobStatus.FAILED || record.getStatus() == JobStatus.TIMEOUT;
    }

    private Map<String, Object> resultMap(JobRecord record) {
        if (record.getResultPayload() == null || record.getResultPayload().isBlank()) {
            return Map.of();
        }
        return jsonUtils.toMap(record.getResultPayload());
    }

    private String stringValue(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value instanceof String string ? string : null;
    }

    private List<String> stringList(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
    }
}
