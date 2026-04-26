package org.example.ggbot.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentResult;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.ai.ChatFallbackPolicy;
import org.example.ggbot.common.IdGenerator;
import org.example.ggbot.common.JsonUtils;
import org.example.ggbot.planner.IntentType;
import org.junit.jupiter.api.Test;

class JobWorkerTest {

    @Test
    void shouldMarkJobSucceededAndPersistResultPayload() {
        AgentService agentService = mock(AgentService.class);
        InMemoryJobService jobService = new InMemoryJobService(new IdGenerator());
        JobRecord record = jobService.create("conversation-1", "user-1", "{\"message\":\"导出聊天记录\"}");
        AgentRequest request = request("导出聊天记录");
        when(agentService.handle(same(request))).thenReturn(
                new AgentResult("task-1", IntentType.CHAT, "最终结果", List.of("artifact-1"))
        );
        JobWorker worker = new JobWorker(agentService, jobService, new JsonUtils(new ObjectMapper()));

        worker.process(record.getJobId(), request);

        JobRecord updated = jobService.get(record.getJobId());
        assertThat(updated.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(updated.getResultPayload())
                .contains("\"taskId\":\"task-1\"")
                .contains("\"replyText\":\"最终结果\"");
        verify(agentService).handle(same(request));
    }

    @Test
    void shouldMarkJobFailedAndPersistFallbackReplyWhenAgentThrows() {
        AgentService agentService = mock(AgentService.class);
        InMemoryJobService jobService = new InMemoryJobService(new IdGenerator());
        JobRecord record = jobService.create("conversation-1", "user-1", "{\"message\":\"导出聊天记录\"}");
        AgentRequest request = request("导出聊天记录");
        when(agentService.handle(same(request))).thenThrow(new RuntimeException("agent boom"));
        JobWorker worker = new JobWorker(agentService, jobService, new JsonUtils(new ObjectMapper()));

        worker.process(record.getJobId(), request);

        JobRecord updated = jobService.get(record.getJobId());
        assertThat(updated.getStatus()).isEqualTo(JobStatus.FAILED);
        assertThat(updated.getErrorMessage()).isEqualTo("agent boom");
        assertThat(updated.getFallbackReply()).isEqualTo(ChatFallbackPolicy.createDefault().fallbackReply());
        verify(agentService).handle(same(request));
    }

    private AgentRequest request(String userInput) {
        return new AgentRequest(
                "conversation-1",
                "user-1",
                userInput,
                AgentChannel.WEB,
                null,
                "conversation-1",
                Map.of()
        );
    }
}
