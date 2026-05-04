package org.example.ggbot.adapter.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.WebContextResponse;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.exception.BadRequestException;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class WebContextController {

    private static final String WEB_USER_KEY_COOKIE = "web_user_key";

    private final IdentityService identityService;

    @GetMapping("/context")
    public ApiResponse<WebContextResponse> context(
            @CookieValue(value = WEB_USER_KEY_COOKIE, required = false) Cookie webUserKeyCookie,
            @RequestParam(required = false) String clientKey,
            HttpServletResponse response
    ) {
        String webUserKey = resolveWebUserKey(webUserKeyCookie, clientKey);
        ResolvedWebUser resolvedUser = identityService.getOrCreateUserByWebSession(webUserKey);
        Cookie cookie = new Cookie(WEB_USER_KEY_COOKIE, webUserKey);
        cookie.setHttpOnly(false);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ApiResponse.success(new WebContextResponse(
                webUserKey,
                resolvedUser.user().getId(),
                resolvedUser.personalOrg().getId()
        ));
    }

    private String resolveWebUserKey(Cookie webUserKeyCookie, String clientKey) {
        if (webUserKeyCookie != null && webUserKeyCookie.getValue() != null && !webUserKeyCookie.getValue().isBlank()) {
            return webUserKeyCookie.getValue();
        }
        if (clientKey != null && !clientKey.isBlank()) {
            return clientKey;
        }
        throw new BadRequestException("web_user_key cookie or clientKey is required");
    }
}
