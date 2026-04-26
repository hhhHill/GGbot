package org.example.ggbot.adapter.web;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.ai.LlmStartupProbeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Data
@RequiredArgsConstructor
public class HealthController {

    private final LlmStartupProbeService llmStartupProbeService;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("app", "GGbot");
        result.put("llmConfigured", llmStartupProbeService.isLlmConfigured());
        result.put("llmReachable", llmStartupProbeService.isLlmReachable());
        result.put("llmMessage", llmStartupProbeService.getLlmMessage());
        return result;
    }
}
