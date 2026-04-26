package org.example.ggbot.job;

import java.util.Collection;

public interface JobService {

    JobRecord create(String conversationId, String userId, String originalRequestPayload);

    JobRecord get(String jobId);

    void markRunning(String jobId, String progressMessage);

    void markSucceeded(String jobId, String resultPayload);

    void markFailed(String jobId, String errorMessage, String fallbackReply);

    void markTimeout(String jobId, String fallbackReply);

    JobRecord retry(String jobId);

    Collection<JobRecord> snapshot();
}
