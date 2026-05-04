package org.example.ggbot.adapter.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.WebCreateSessionRequest;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.session.WebSession;
import org.example.ggbot.session.WebSessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class WebSessionController {

    private final WebSessionService sessionService;

    @PostMapping
    public ApiResponse<WebSession> createSession(@RequestBody WebCreateSessionRequest request) {
        return ApiResponse.success(sessionService.createSession(request.getUserId()));
    }

    @GetMapping
    public ApiResponse<List<WebSession>> listSessions(@RequestParam String userId) {
        return ApiResponse.success(sessionService.listSessions(userId));
    }

    @GetMapping("/{sessionId}")
    public ApiResponse<WebSession> getSession(
            @PathVariable String sessionId,
            @RequestParam String userId
    ) {
        return ApiResponse.success(sessionService.getSession(userId, sessionId));
    }
}
