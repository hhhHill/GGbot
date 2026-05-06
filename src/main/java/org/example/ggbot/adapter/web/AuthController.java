package org.example.ggbot.adapter.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.AuthRequest;
import org.example.ggbot.adapter.web.dto.WebContextResponse;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.service.auth.LocalAuthService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    static final String WEB_AUTH_TOKEN_COOKIE = "web_auth_token";

    private final LocalAuthService localAuthService;

    @PostMapping("/register")
    public ApiResponse<WebContextResponse> register(
            @Valid @RequestBody AuthRequest request,
            HttpServletResponse response
    ) {
        LocalAuthService.AuthenticatedSession session = localAuthService.register(request.username(), request.password());
        response.addCookie(createAuthCookie(session.token(), 30 * 24 * 60 * 60));
        return ApiResponse.success(toResponse(session));
    }

    @PostMapping("/login")
    public ApiResponse<WebContextResponse> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletResponse response
    ) {
        LocalAuthService.AuthenticatedSession session = localAuthService.login(request.username(), request.password());
        response.addCookie(createAuthCookie(session.token(), 30 * 24 * 60 * 60));
        return ApiResponse.success(toResponse(session));
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(
            @CookieValue(value = WEB_AUTH_TOKEN_COOKIE, required = false) Cookie authCookie,
            HttpServletResponse response
    ) {
        localAuthService.logout(authCookie == null ? null : authCookie.getValue());
        response.addCookie(createAuthCookie("", 0));
        return ApiResponse.success("logged_out");
    }

    private Cookie createAuthCookie(String token, int maxAgeSeconds) {
        Cookie cookie = new Cookie(WEB_AUTH_TOKEN_COOKIE, token);
        cookie.setHttpOnly(false);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSeconds);
        return cookie;
    }

    private WebContextResponse toResponse(LocalAuthService.AuthenticatedSession session) {
        return new WebContextResponse(
                null,
                session.resolvedUser().user().getId(),
                session.resolvedUser().personalOrg().getId(),
                true,
                session.username()
        );
    }
}
