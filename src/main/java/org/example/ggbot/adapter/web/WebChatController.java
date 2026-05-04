package org.example.ggbot.adapter.web;

import jakarta.servlet.http.Cookie;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.WebChatSendRequest;
import org.example.ggbot.adapter.web.dto.WebChatSendResponse;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.enums.MessageRole;
import org.example.ggbot.exception.BadRequestException;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.service.context.PersistentConversationContextService;
import org.example.ggbot.service.conversation.ConversationService;
import org.example.ggbot.service.dto.ConversationContext;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
import org.example.ggbot.service.organization.OrganizationService;
import org.example.ggbot.service.subject.SubjectService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/chat")
@RequiredArgsConstructor
public class WebChatController {

    private final IdentityService identityService;
    private final OrganizationService organizationService;
    private final SubjectService subjectService;
    private final ConversationService conversationService;
    private final PersistentConversationContextService contextService;
    private final AgentService agentService;

    @PostMapping("/send")
    public ApiResponse<WebChatSendResponse> send(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @RequestBody WebChatSendRequest request
    ) {
        ResolvedWebUser resolvedUser = identityService.getOrCreateUserByWebSession(resolveWebUserKey(webUserKeyCookie, request));
        Long userId = resolvedUser.user().getId();
        Long currentOrgId = resolveOrgId(resolvedUser, request.getOrgId());
        SubjectEntity subject = subjectService.getOrCreateUserSubject(userId, currentOrgId);
        ConversationEntity conversation = resolveConversation(currentOrgId, subject.getId(), userId, request);

        conversationService.addMessage(
                currentOrgId,
                conversation.getId(),
                userId,
                MessageRole.USER,
                request.getMessageContent(),
                "text",
                null
        );

        ConversationContext context = contextService.buildContext(currentOrgId, subject.getId(), conversation.getId());
        String reply = agentService.replyWithPersistentContext(context, request.getMessageContent());

        conversationService.addMessage(
                currentOrgId,
                conversation.getId(),
                null,
                MessageRole.ASSISTANT,
                reply,
                "text",
                null
        );

        return ApiResponse.success(new WebChatSendResponse(
                currentOrgId,
                subject.getId(),
                conversation.getId(),
                reply
        ));
    }

    private String resolveWebUserKey(Cookie webUserKeyCookie, WebChatSendRequest request) {
        if (webUserKeyCookie != null && webUserKeyCookie.getValue() != null && !webUserKeyCookie.getValue().isBlank()) {
            return webUserKeyCookie.getValue();
        }
        if (request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return request.getSessionId();
        }
        throw new BadRequestException("web_user_key cookie or sessionId is required");
    }

    private Long resolveOrgId(ResolvedWebUser resolvedUser, Long requestedOrgId) {
        if (requestedOrgId == null) {
            return resolvedUser.personalOrg().getId();
        }
        organizationService.checkUserActiveInOrg(resolvedUser.user().getId(), requestedOrgId);
        return requestedOrgId;
    }

    private ConversationEntity resolveConversation(
            Long orgId,
            Long subjectId,
            Long userId,
            WebChatSendRequest request
    ) {
        if (request.getConversationId() != null) {
            return ConversationEntity.builder()
                    .id(request.getConversationId())
                    .orgId(orgId)
                    .subjectId(subjectId)
                    .build();
        }
        String title = request.getMessageContent();
        return conversationService.createConversation(orgId, subjectId, "web", title, userId);
    }
}
