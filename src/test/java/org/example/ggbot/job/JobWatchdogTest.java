package org.example.ggbot.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.example.ggbot.common.IdGenerator;
import org.junit.jupiter.api.Test;

class JobWatchdogTest {

    @Test
    void shouldMarkRunningJobTimedOutWhenNoProgressWithinTimeoutWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-04-26T12:00:00Z"));
        InMemoryJobService service = new InMemoryJobService(new IdGenerator(), clock);
        JobRecord record = service.create("conversation-1", "user-1", "{\"message\":\"导出聊天记录\"}");
        service.markRunning(record.getJobId(), "正在调用模型");
        clock.advance(Duration.ofSeconds(120));

        JobWatchdog watchdog = new JobWatchdog(service, Duration.ofSeconds(60), clock);
        watchdog.scan();

        JobRecord updated = service.get(record.getJobId());
        assertThat(updated.getStatus()).isEqualTo(JobStatus.TIMEOUT);
        assertThat(updated.getFallbackReply()).isEqualTo("我们遇到了一些问题，稍后再试一次。");
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
