package org.example.ggbot.adapter.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.WebContextResponse;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.service.auth.WebUserContext;
import org.example.ggbot.service.auth.WebUserContextResolver;
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
    private static final String WEB_AUTH_TOKEN_COOKIE = "web_auth_token";

    private final WebUserContextResolver webUserContextResolver;

    @GetMapping("/context")
    public ApiResponse<WebContextResponse> context(
            @CookieValue(value = WEB_USER_KEY_COOKIE, required = false) Cookie webUserKeyCookie,
            @CookieValue(value = WEB_AUTH_TOKEN_COOKIE, required = false) Cookie authCookie,
            @RequestParam(required = false) String clientKey,
            HttpServletResponse response
    ) {
        WebUserContext context = webUserContextResolver.resolve(authCookie, webUserKeyCookie, clientKey, true);
        Cookie cookie = new Cookie(WEB_USER_KEY_COOKIE, context.webUserKey());
        cookie.setHttpOnly(false);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ApiResponse.success(new WebContextResponse(
                context.webUserKey(),
                context.resolvedUser().user().getId(),
                context.resolvedUser().personalOrg().getId(),
                context.authenticated(),
                context.loginName()
        ));
    }
}
