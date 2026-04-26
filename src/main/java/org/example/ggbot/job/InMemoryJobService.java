package org.example.ggbot.job;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.common.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InMemoryJobService implements JobService {

    private final IdGenerator idGenerator;
    private final Clock clock;
    private final ConcurrentMap<String, JobRecord> jobs = new ConcurrentHashMap<>();

    @Autowired
    public InMemoryJobService(IdGenerator idGenerator) {
        this(idGenerator, Clock.systemUTC());
    }

    InMemoryJobService(IdGenerator idGenerator, Clock clock) {
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Override
    public JobRecord create(String conversationId, String userId, String originalRequestPayload) {
        JobRecord record = JobRecord.queued(
                idGenerator.nextId("job"),
                conversationId,
                userId,
                originalRequestPayload,
                now()
        );
        jobs.put(record.getJobId(), record);
        return record;
    }

    @Override
    public JobRecord get(String jobId) {
        JobRecord record = jobs.get(jobId);
        if (record == null) {
            throw missingJob(jobId);
        }
        return record;
    }

    @Override
    public void markRunning(String jobId, String progressMessage) {
        replace(jobId, record -> record.markRunning(progressMessage, now()));
    }

    @Override
    public void markSucceeded(String jobId, String resultPayload) {
        replace(jobId, record -> record.markSucceeded(resultPayload, now()));
    }

    @Override
    public void markFailed(String jobId, String errorMessage, String fallbackReply) {
        replace(jobId, record -> record.markFailed(errorMessage, fallbackReply, now()));
    }

    @Override
    public void markTimeout(String jobId, String fallbackReply) {
        replace(jobId, record -> record.markTimeout(fallbackReply, now()));
    }

    @Override
    public JobRecord retry(String jobId) {
        JobRecord existing = get(jobId);
        JobRecord retried = existing.retryAs(idGenerator.nextId("job"), now());
        jobs.put(retried.getJobId(), retried);
        return retried;
    }

    @Override
    public Collection<JobRecord> snapshot() {
        return List.copyOf(jobs.values());
    }

    private void replace(String jobId, java.util.function.UnaryOperator<JobRecord> updater) {
        JobRecord updated = jobs.computeIfPresent(jobId, (ignored, record) -> updater.apply(record));
        if (updated == null) {
            throw missingJob(jobId);
        }
    }

    private IllegalArgumentException missingJob(String jobId) {
        return new IllegalArgumentException("Job not found: " + jobId);
    }

    private Instant now() {
        return clock.instant();
    }
}
