package org.example.ggbot.adapter.web;

import jakarta.servlet.http.Cookie;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.OrganizationResponse;
import org.example.ggbot.adapter.web.dto.SwitchOrganizationRequest;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.exception.BadRequestException;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
import org.example.ggbot.service.organization.OrganizationService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orgs")
@RequiredArgsConstructor
public class OrganizationController {

    private final IdentityService identityService;
    private final OrganizationService organizationService;

    @GetMapping
    public ApiResponse<List<OrganizationResponse>> listOrganizations(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie
    ) {
        ResolvedWebUser resolvedUser = identityService.getOrCreateUserByWebSession(resolveWebUserKey(webUserKeyCookie));
        List<OrganizationResponse> organizations = organizationService.listActiveOrganizations(resolvedUser.user().getId())
                .stream()
                .map(org -> new OrganizationResponse(
                        org.getId(),
                        org.getName(),
                        org.getProvider() == null ? null : org.getProvider().name(),
                        org.getOrgType() == null ? null : org.getOrgType().name(),
                        org.getTenantKey(),
                        org.getStatus() == null ? null : org.getStatus().name()
                ))
                .toList();
        return ApiResponse.success(organizations);
    }

    @PostMapping("/switch")
    public ApiResponse<String> switchOrganization(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @RequestBody SwitchOrganizationRequest request
    ) {
        ResolvedWebUser resolvedUser = identityService.getOrCreateUserByWebSession(resolveWebUserKey(webUserKeyCookie));
        if (request.getOrgId() == null) {
            throw new BadRequestException("orgId is required");
        }
        organizationService.checkUserActiveInOrg(resolvedUser.user().getId(), request.getOrgId());
        return ApiResponse.success("switched");
    }

    private String resolveWebUserKey(Cookie webUserKeyCookie) {
        if (webUserKeyCookie != null && webUserKeyCookie.getValue() != null && !webUserKeyCookie.getValue().isBlank()) {
            return webUserKeyCookie.getValue();
        }
        throw new BadRequestException("web_user_key cookie is required");
    }
}
