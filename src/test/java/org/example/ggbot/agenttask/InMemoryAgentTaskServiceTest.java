package org.example.ggbot.agenttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.common.IdGenerator;
import org.junit.jupiter.api.Test;

class InMemoryAgentTaskServiceTest {

    @Test
    void shouldCreatePendingTaskWithRequestPayload() {
        InMemoryAgentTaskService service = new InMemoryAgentTaskService(new IdGenerator());

        AgentTaskRecord task = service.createTask(request("hello"), "web", null);

        assertThat(task.getTaskId()).startsWith("agent-task-");
        assertThat(task.getStatus()).isEqualTo(AgentTaskStatus.PENDING);
        assertThat(task.getSessionId()).isEqualTo("session-1");
        assertThat(task.getUserId()).isEqualTo("user-1");
        assertThat(task.getInput()).isEqualTo("hello");
        assertThat(task.getRetryCount()).isZero();
    }

    @Test
    void shouldReturnExistingTaskForSameExternalEventId() {
        InMemoryAgentTaskService service = new InMemoryAgentTaskService(new IdGenerator());

        AgentTaskCreationResult first = service.createOrGetByExternalEventId(request("hello"), "feishu", "event-1");
        AgentTaskCreationResult second = service.createOrGetByExternalEventId(request("hello again"), "feishu", "event-1");

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.task().getTaskId()).isEqualTo(first.task().getTaskId());
        assertThat(service.findByTaskId(first.task().getTaskId()).getInput()).isEqualTo("hello");
    }

    @Test
    void shouldUpdateTaskStatusAcrossLifecycle() {
        InMemoryAgentTaskService service = new InMemoryAgentTaskService(new IdGenerator());
        AgentTaskRecord task = service.createTask(request("hello"), "web", null);

        service.markRunning(task.getTaskId());
        service.markRetrying(task.getTaskId(), 1, "temporary failure");
        service.markSuccess(task.getTaskId(), "done");

        AgentTaskRecord updated = service.findByTaskId(task.getTaskId());
        assertThat(updated.getStatus()).isEqualTo(AgentTaskStatus.SUCCESS);
        assertThat(updated.getResult()).isEqualTo("done");
        assertThat(updated.getRetryCount()).isEqualTo(1);
        assertThat(updated.getStartedAt()).isNotNull();
        assertThat(updated.getFinishedAt()).isNotNull();
    }

    @Test
    void shouldResetFailedTaskForManualRetry() {
        InMemoryAgentTaskService service = new InMemoryAgentTaskService(new IdGenerator());
        AgentTaskRecord task = service.createTask(request("hello"), "web", null);
        service.markFailed(task.getTaskId(), "boom");

        AgentTaskRecord retried = service.retry(task.getTaskId());

        assertThat(retried.getStatus()).isEqualTo(AgentTaskStatus.PENDING);
        assertThat(retried.getRetryCount()).isZero();
        assertThat(retried.getErrorMessage()).isNull();
    }

    @Test
    void shouldRejectRetryForNonFailedTask() {
        InMemoryAgentTaskService service = new InMemoryAgentTaskService(new IdGenerator());
        AgentTaskRecord task = service.createTask(request("hello"), "web", null);

        assertThatThrownBy(() -> service.retry(task.getTaskId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(task.getTaskId());
    }

    private AgentRequest request(String input) {
        return new AgentRequest("session-1", "user-1", input, AgentChannel.WEB, null, "session-1", Map.of());
    }
}
