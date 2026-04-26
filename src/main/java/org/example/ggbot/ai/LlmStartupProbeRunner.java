package org.example.ggbot.ai;

import java.util.concurrent.Executor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 在应用启动完成后异步触发一次大模型连通性探测。
 */
@Component
@Data
@RequiredArgsConstructor
public class LlmStartupProbeRunner {

    private final LlmConfigurationDiagnosticsService llmConfigurationDiagnosticsService;
    private final LlmStartupProbeService llmStartupProbeService;
    private final Executor llmProbeExecutor;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        llmConfigurationDiagnosticsService.logResolvedConfiguration();
        llmProbeExecutor.execute(llmStartupProbeService::runStartupProbe);
    }
}
