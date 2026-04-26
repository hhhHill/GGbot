package org.example.ggbot.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentResult;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.common.JsonUtils;
import org.example.ggbot.job.AsyncExecutionDecider;
import org.example.ggbot.job.AsyncExecutionMode;
import org.example.ggbot.job.JobRecord;
import org.example.ggbot.job.JobService;
import org.example.ggbot.job.JobWorker;
import org.example.ggbot.planner.IntentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WebJobControllerTest {

    @Test
    void shouldReturnSyncChatResponseForShortRequests() throws Exception {
        AgentService agentService = mock(AgentService.class);
        AsyncExecutionDecider decider = mock(AsyncExecutionDecider.class);
        JobService jobService = mock(JobService.class);
        JobWorker jobWorker = mock(JobWorker.class);
        Executor executor = Runnable::run;
        when(decider.decide(any())).thenReturn(AsyncExecutionMode.SYNC);
        when(agentService.handle(any())).thenReturn(
                new AgentResult("task-1", IntentType.CHAT, "同步结果", List.of("artifact-1"))
        );

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAgentController(
                agentService,
                decider,
                jobService,
                jobWorker,
                executor,
                new JsonUtils(new ObjectMapper())
        )).build();

        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-1",
                                  "userId": "user-1",
                                  "message": "你好"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.replyText").value("同步结果"))
                .andExpect(jsonPath("$.data.intentType").value("CHAT"));
    }

    @Test
    void shouldReturnAcceptedResponseAndDispatchBackgroundJobForAsyncRequests() throws Exception {
        AgentService agentService = mock(AgentService.class);
        AsyncExecutionDecider decider = mock(AsyncExecutionDecider.class);
        JobService jobService = mock(JobService.class);
        JobWorker jobWorker = mock(JobWorker.class);
        Executor executor = Runnable::run;
        JobRecord queued = JobRecord.queued(
                "job-1",
                "conversation-1",
                "user-1",
                "{\"conversationId\":\"conversation-1\",\"userId\":\"user-1\",\"message\":\"导出聊天记录\"}",
                Instant.parse("2026-04-26T12:00:00Z")
        );
        when(decider.decide(any())).thenReturn(AsyncExecutionMode.ASYNC);
        when(jobService.create(eq("conversation-1"), eq("user-1"), any())).thenReturn(queued);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAgentController(
                agentService,
                decider,
                jobService,
                jobWorker,
                executor,
                new JsonUtils(new ObjectMapper())
        )).build();

        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "conversation-1",
                                  "userId": "user-1",
                                  "message": "导出聊天记录"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.jobId").value("job-1"))
                .andExpect(jsonPath("$.data.status").value("QUEUED"));

        verify(jobWorker).process(eq("job-1"), any(AgentRequest.class));
    }

    @Test
    void shouldExposeJobStatusWithReplyTextAndRetryCapability() throws Exception {
        AgentService agentService = mock(AgentService.class);
        AsyncExecutionDecider decider = mock(AsyncExecutionDecider.class);
        JobService jobService = mock(JobService.class);
        JobWorker jobWorker = mock(JobWorker.class);
        Executor executor = Runnable::run;
        JobRecord succeeded = JobRecord.queued(
                        "job-1",
                        "conversation-1",
                        "user-1",
                        "{\"conversationId\":\"conversation-1\",\"userId\":\"user-1\",\"message\":\"导出聊天记录\"}",
                        Instant.parse("2026-04-26T12:00:00Z")
                )
                .markSucceeded(
                        "{\"taskId\":\"task-1\",\"intentType\":\"CHAT\",\"replyText\":\"最终结果\",\"artifactSummaries\":[]}",
                        Instant.parse("2026-04-26T12:00:10Z")
                );
        when(jobService.get("job-1")).thenReturn(succeeded);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAgentController(
                agentService,
                decider,
                jobService,
                jobWorker,
                executor,
                new JsonUtils(new ObjectMapper())
        )).build();

        mockMvc.perform(get("/api/agent/jobs/job-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value("job-1"))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.replyText").value("最终结果"))
                .andExpect(jsonPath("$.data.canRetry").value(false));
    }

    @Test
    void shouldCreateNewJobAndDispatchWorkerWhenRetryingFailedJob() throws Exception {
        AgentService agentService = mock(AgentService.class);
        AsyncExecutionDecider decider = mock(AsyncExecutionDecider.class);
        JobService jobService = mock(JobService.class);
        JobWorker jobWorker = mock(JobWorker.class);
        Executor executor = Runnable::run;
        JobRecord retried = JobRecord.queued(
                "job-2",
                "conversation-1",
                "user-1",
                "{\"conversationId\":\"conversation-1\",\"userId\":\"user-1\",\"message\":\"导出聊天记录\"}",
                Instant.parse("2026-04-26T12:00:00Z")
        );
        when(jobService.retry("job-1")).thenReturn(retried);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAgentController(
                agentService,
                decider,
                jobService,
                jobWorker,
                executor,
                new JsonUtils(new ObjectMapper())
        )).build();

        mockMvc.perform(post("/api/agent/jobs/job-1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value("job-2"))
                .andExpect(jsonPath("$.data.status").value("QUEUED"));

        verify(jobWorker).process(eq("job-2"), any(AgentRequest.class));
    }
}
