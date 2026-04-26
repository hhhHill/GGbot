package org.example.ggbot.job;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobWatchdog {

    private static final String TIMEOUT_FALLBACK_REPLY = "我们遇到了一些问题，稍后再试一次。";

    private final JobService jobService;
    private final Duration timeout;
    private final Clock clock;

    @Autowired
    public JobWatchdog(JobService jobService) {
        this(jobService, Duration.ofSeconds(60), Clock.systemUTC());
    }

    JobWatchdog(JobService jobService, Duration timeout, Clock clock) {
        this.jobService = jobService;
        this.timeout = timeout;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 10000L)
    public void scan() {
        Instant threshold = clock.instant().minus(timeout);
        for (JobRecord record : jobService.snapshot()) {
            if (isActive(record) && record.getLastProgressAt().isBefore(threshold)) {
                jobService.markTimeout(record.getJobId(), TIMEOUT_FALLBACK_REPLY);
            }
        }
    }

    private boolean isActive(JobRecord record) {
        return record.getStatus() == JobStatus.QUEUED || record.getStatus() == JobStatus.RUNNING;
    }
}
