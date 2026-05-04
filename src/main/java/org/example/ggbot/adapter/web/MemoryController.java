package org.example.ggbot.adapter.web;

import jakarta.servlet.http.Cookie;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.MemoryResponse;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.exception.BadRequestException;
import org.example.ggbot.service.access.AccessControlService;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
import org.example.ggbot.service.memory.MemoryService;
import org.example.ggbot.service.organization.OrganizationService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MemoryController {

    private final IdentityService identityService;
    private final OrganizationService organizationService;
    private final AccessControlService accessControlService;
    private final MemoryService memoryService;

    @GetMapping("/api/memory")
    public ApiResponse<List<MemoryResponse>> listAccessibleMemory(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @RequestParam Long orgId
    ) {
        ResolvedWebUser resolvedUser = identityService.getOrCreateUserByWebSession(resolveWebUserKey(webUserKeyCookie));
        organizationService.checkUserActiveInOrg(resolvedUser.user().getId(), orgId);
        List<MemoryResponse> memory = memoryService.listAccessibleMemory(resolvedUser.user().getId(), orgId).stream()
                .map(entry -> new MemoryResponse(
                        entry.getId(),
                        entry.getOrgId(),
                        entry.getSubjectId(),
                        entry.getMemoryType() == null ? null : entry.getMemoryType().name(),
                        entry.getScope() == null ? null : entry.getScope().name(),
                        entry.getContent(),
                        entry.getSourceConversationId(),
                        entry.getCreatedByUserId(),
                        entry.getUpdatedAt()
                ))
                .toList();
        return ApiResponse.success(memory);
    }

    @GetMapping("/api/subjects/{subjectId}/memory")
    public ApiResponse<List<MemoryResponse>> listSubjectMemory(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @RequestParam Long orgId,
            @PathVariable Long subjectId
    ) {
        ResolvedWebUser resolvedUser = identityService.getOrCreateUserByWebSession(resolveWebUserKey(webUserKeyCookie));
        organizationService.checkUserActiveInOrg(resolvedUser.user().getId(), orgId);
        accessControlService.checkCanAccessSubject(resolvedUser.user().getId(), orgId, subjectId);
        List<MemoryResponse> memory = memoryService.listSubjectMemory(orgId, subjectId).stream()
                .map(entry -> new MemoryResponse(
                        entry.getId(),
                        entry.getOrgId(),
                        entry.getSubjectId(),
                        entry.getMemoryType() == null ? null : entry.getMemoryType().name(),
                        entry.getScope() == null ? null : entry.getScope().name(),
                        entry.getContent(),
                        entry.getSourceConversationId(),
                        entry.getCreatedByUserId(),
                        entry.getUpdatedAt()
                ))
                .toList();
        return ApiResponse.success(memory);
    }

    private String resolveWebUserKey(Cookie webUserKeyCookie) {
        if (webUserKeyCookie != null && webUserKeyCookie.getValue() != null && !webUserKeyCookie.getValue().isBlank()) {
            return webUserKeyCookie.getValue();
        }
        throw new BadRequestException("web_user_key cookie is required");
    }
}
