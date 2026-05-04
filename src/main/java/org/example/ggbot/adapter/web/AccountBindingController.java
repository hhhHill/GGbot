package org.example.ggbot.adapter.web;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.CreateBindTokenRequest;
import org.example.ggbot.adapter.web.dto.CreateBindTokenResponse;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.exception.BadRequestException;
import org.example.ggbot.service.binding.AccountBindingService;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
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

    private final IdentityService identityService;
    private final AccountBindingService accountBindingService;
    private final OrganizationService organizationService;

    @PostMapping("/token")
    public ApiResponse<CreateBindTokenResponse> createBindToken(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @RequestBody(required = false) CreateBindTokenRequest request
    ) {
        ResolvedWebUser resolvedUser = identityService.getOrCreateUserByWebSession(resolveWebUserKey(webUserKeyCookie));
        Long currentOrgId = request != null && request.getOrgId() != null
                ? request.getOrgId()
                : resolvedUser.personalOrg().getId();
        organizationService.checkUserActiveInOrg(resolvedUser.user().getId(), currentOrgId);
        String token = accountBindingService.createBindToken(resolvedUser.user().getId(), currentOrgId);
        return ApiResponse.success(new CreateBindTokenResponse(token));
    }

    private String resolveWebUserKey(Cookie webUserKeyCookie) {
        if (webUserKeyCookie != null && webUserKeyCookie.getValue() != null && !webUserKeyCookie.getValue().isBlank()) {
            return webUserKeyCookie.getValue();
        }
        throw new BadRequestException("web_user_key cookie is required");
    }
}
