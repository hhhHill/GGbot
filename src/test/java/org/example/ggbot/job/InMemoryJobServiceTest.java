package org.example.ggbot.job;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.lang.reflect.Method;
import java.util.Collection;
import org.example.ggbot.common.IdGenerator;
import org.junit.jupiter.api.Test;

class InMemoryJobServiceTest {

    @Test
    void shouldCreateQueuedJobAndUpdateProgressWhenRunning() {
        InMemoryJobService service = new InMemoryJobService(new IdGenerator());

        JobRecord created = service.create("conversation-1", "user-1", "{\"message\":\"hello\"}");

        assertThat(created.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(created.getProgressMessage()).isEqualTo("已接收请求");

        service.markRunning(created.getJobId(), "正在调用模型");
        JobRecord loaded = service.get(created.getJobId());

        assertThat(loaded.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(loaded.getProgressMessage()).isEqualTo("正在调用模型");
    }

    @Test
    void shouldCreateNewJobIdAndCopyOriginalPayloadWhenRetrying() {
        InMemoryJobService service = new InMemoryJobService(new IdGenerator());

        JobRecord first = service.create("conversation-1", "user-1", "{\"message\":\"hello\"}");

        JobRecord retried = service.retry(first.getJobId());

        assertThat(retried.getJobId()).isNotEqualTo(first.getJobId());
        assertThat(retried.getOriginalRequestPayload()).isEqualTo(first.getOriginalRequestPayload());
        assertThat(retried.getConversationId()).isEqualTo(first.getConversationId());
        assertThat(retried.getUserId()).isEqualTo(first.getUserId());
        assertThat(retried.getStatus()).isEqualTo(JobStatus.QUEUED);
    }

    @Test
    void shouldRejectMissingJobOperationsConsistently() {
        InMemoryJobService service = new InMemoryJobService(new IdGenerator());

        assertThatThrownBy(() -> service.get("job-missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("job-missing");
        assertThatThrownBy(() -> service.retry("job-missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("job-missing");
        assertThatThrownBy(() -> service.markRunning("job-missing", "正在调用模型"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("job-missing");
        assertThatThrownBy(() -> service.markSucceeded("job-missing", "{\"reply\":\"ok\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("job-missing");
        assertThatThrownBy(() -> service.markFailed("job-missing", "boom", "fallback"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("job-missing");
        assertThatThrownBy(() -> service.markTimeout("job-missing", "fallback"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("job-missing");
    }

    @Test
    void shouldReturnImmutableSnapshotView() {
        InMemoryJobService service = new InMemoryJobService(new IdGenerator());
        JobRecord created = service.create("conversation-1", "user-1", "{\"message\":\"hello\"}");

        Collection<JobRecord> snapshot = service.snapshot();
        JobRecord snapshotRecord = snapshot.iterator().next();

        service.markRunning(created.getJobId(), "正在调用模型");

        assertThatThrownBy(() -> snapshot.clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(snapshotRecord.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(service.get(created.getJobId()).getStatus()).isEqualTo(JobStatus.RUNNING);
    }

    @Test
    void shouldExposeJobRecordAsImmutableValueObject() {
        assertThat(Arrays.stream(JobRecord.class.getMethods()).map(Method::getName).toList())
                .noneMatch(name -> name.startsWith("set"));
    }
}
