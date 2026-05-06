package org.example.ggbot.adapter.web;

import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import java.util.List;
import org.example.ggbot.adapter.web.dto.RenameConversationRequest;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.web.dto.ConversationSummaryResponse;
import org.example.ggbot.adapter.web.dto.MessageResponse;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.service.access.AccessControlService;
import org.example.ggbot.service.conversation.ConversationService;
import org.example.ggbot.service.auth.WebUserContext;
import org.example.ggbot.service.auth.WebUserContextResolver;
import org.example.ggbot.service.organization.OrganizationService;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final WebUserContextResolver webUserContextResolver;
    private final OrganizationService organizationService;
    private final AccessControlService accessControlService;
    private final ConversationService conversationService;

    @GetMapping
    public ApiResponse<List<ConversationSummaryResponse>> listConversations(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @CookieValue(value = "web_auth_token", required = false) Cookie authCookie,
            @RequestParam Long orgId
    ) {
        WebUserContext context = webUserContextResolver.resolve(authCookie, webUserKeyCookie, null, false);
        organizationService.checkUserActiveInOrg(context.resolvedUser().user().getId(), orgId);
        List<ConversationSummaryResponse> conversations = conversationService.listAccessibleConversations(
                        context.resolvedUser().user().getId(), orgId)
                .stream()
                .map(conversation -> new ConversationSummaryResponse(
                        conversation.getId(),
                        conversation.getOrgId(),
                        conversation.getSubjectId(),
                        conversation.getTitle(),
                        conversation.getSource(),
                        conversation.getStatus() == null ? null : conversation.getStatus().name(),
                        conversation.getLastMessageAt()
                ))
                .toList();
        return ApiResponse.success(conversations);
    }

    @GetMapping("/{conversationId}/messages")
    public ApiResponse<List<MessageResponse>> listMessages(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @CookieValue(value = "web_auth_token", required = false) Cookie authCookie,
            @RequestParam Long orgId,
            @PathVariable Long conversationId
    ) {
        WebUserContext context = webUserContextResolver.resolve(authCookie, webUserKeyCookie, null, false);
        organizationService.checkUserActiveInOrg(context.resolvedUser().user().getId(), orgId);
        accessControlService.checkCanAccessConversation(context.resolvedUser().user().getId(), orgId, conversationId);
        List<MessageResponse> messages = conversationService.listMessages(orgId, conversationId)
                .stream()
                .map(message -> new MessageResponse(
                        message.getId(),
                        message.getConversationId(),
                        message.getSenderUserId(),
                        message.getRole() == null ? null : message.getRole().name(),
                        message.getContent(),
                        message.getMessageType(),
                        message.getCreatedAt()
                ))
                .toList();
        return ApiResponse.success(messages);
    }

    @PatchMapping("/{conversationId}/title")
    public ApiResponse<ConversationSummaryResponse> renameConversation(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @CookieValue(value = "web_auth_token", required = false) Cookie authCookie,
            @RequestParam Long orgId,
            @PathVariable Long conversationId,
            @Valid @RequestBody RenameConversationRequest request
    ) {
        WebUserContext context = webUserContextResolver.resolve(authCookie, webUserKeyCookie, null, false);
        organizationService.checkUserActiveInOrg(context.resolvedUser().user().getId(), orgId);
        accessControlService.checkCanAccessConversation(context.resolvedUser().user().getId(), orgId, conversationId);
        var conversation = conversationService.renameConversation(orgId, conversationId, request.title());
        return ApiResponse.success(new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getOrgId(),
                conversation.getSubjectId(),
                conversation.getTitle(),
                conversation.getSource(),
                conversation.getStatus() == null ? null : conversation.getStatus().name(),
                conversation.getLastMessageAt()
        ));
    }

    @DeleteMapping("/{conversationId}")
    public ApiResponse<Boolean> deleteConversation(
            @CookieValue(value = "web_user_key", required = false) Cookie webUserKeyCookie,
            @CookieValue(value = "web_auth_token", required = false) Cookie authCookie,
            @RequestParam Long orgId,
            @PathVariable Long conversationId
    ) {
        WebUserContext context = webUserContextResolver.resolve(authCookie, webUserKeyCookie, null, false);
        organizationService.checkUserActiveInOrg(context.resolvedUser().user().getId(), orgId);
        accessControlService.checkCanAccessConversation(context.resolvedUser().user().getId(), orgId, conversationId);
        conversationService.deleteConversation(orgId, conversationId);
        return ApiResponse.success(Boolean.TRUE);
    }
}
