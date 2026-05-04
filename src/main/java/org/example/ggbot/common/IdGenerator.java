package org.example.ggbot.common;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@RequiredArgsConstructor
public class IdGenerator {

    private final AtomicLong longSequence = new AtomicLong(System.currentTimeMillis());

    public String nextId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "");
    }

    public long nextLongId() {
        return longSequence.incrementAndGet();
    }
}
