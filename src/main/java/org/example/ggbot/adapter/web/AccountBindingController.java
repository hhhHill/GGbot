package org.example.ggbot.adapter.web;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.CreateBindTokenRequest;
import org.example.ggbot.adapter.web.dto.CreateBindTokenResponse;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.exception.ForbiddenException;
import org.example.ggbot.service.binding.AccountBindingService;
import org.example.ggbot.service.auth.WebUserContext;
import org.example.ggbot.service.auth.WebUserContextResolver;
import org.example.ggbot.service.organization.OrganizationService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bind/feishu")
@RequiredArgsConstructor
public class AccountBindingController {

    private final WebUserContextResolver webUserContextResolver;
    private final AccountBindingService accountBindingService;
    private final OrganizationService organizationService;

    @PostMapping("/token")
    public ApiResponse<CreateBindTokenResponse> createBindToken(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @CookieValue(value = "web_auth_token", required = false) Cookie authCookie,
            @RequestBody(required = false) CreateBindTokenRequest request
    ) {
        WebUserContext context = webUserContextResolver.resolve(authCookie, webUserKeyCookie, null, false);
        if (!context.authenticated()) {
            throw new ForbiddenException("请先登录后再生成飞书绑定码");
        }
        Long currentOrgId = request != null && request.getOrgId() != null
                ? request.getOrgId()
                : context.resolvedUser().personalOrg().getId();
        organizationService.checkUserActiveInOrg(context.resolvedUser().user().getId(), currentOrgId);
        String token = accountBindingService.createBindToken(context.resolvedUser().user().getId(), currentOrgId);
        return ApiResponse.success(new CreateBindTokenResponse(token));
    }
}
