package org.example.ggbot.adapter.feishu;

import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.enums.MessageRole;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.service.context.PersistentConversationContextService;
import org.example.ggbot.service.conversation.ConversationService;
import org.example.ggbot.service.dto.ConversationContext;
import org.example.ggbot.service.dto.ResolvedFeishuUser;
import org.example.ggbot.service.identity.IdentityService;
import org.example.ggbot.service.subject.SubjectService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeishuMessageHandler {

    private final IdentityService identityService;
    private final SubjectService subjectService;
    private final ConversationService conversationService;
    private final PersistentConversationContextService contextService;
    private final AgentService agentService;
    private final FeishuMessageClient feishuMessageClient;

    public String handle(FeishuInboundMessage message) {
        ResolvedFeishuUser resolvedUser = identityService.getOrCreateUserByFeishuOpenId(
                message.openId(),
                message.tenantKey(),
                message.tenantName(),
                message.senderNickname(),
                message.senderAvatar()
        );
        Long orgId = resolvedUser.org().getId();
        Long userId = resolvedUser.user().getId();
        SubjectEntity subject = resolveSubject(message, orgId, userId);

        ConversationEntity conversation = conversationService.getOrCreateActiveConversation(
                orgId, subject.getId(), "feishu", userId
        );
        conversationService.addMessage(
                orgId,
                conversation.getId(),
                userId,
                MessageRole.USER,
                message.messageContent(),
                "text",
                message.messageId()
        );

        ConversationContext context = contextService.buildContext(orgId, subject.getId(), conversation.getId());
        String reply = agentService.replyWithPersistentContext(context, message.messageContent());

        conversationService.addMessage(
                orgId,
                conversation.getId(),
                null,
                MessageRole.ASSISTANT,
                reply,
                "text",
                null
        );
        feishuMessageClient.sendText(message.chatId(), reply);
        return reply;
    }

    private SubjectEntity resolveSubject(FeishuInboundMessage message, Long orgId, Long userId) {
        if ("group".equalsIgnoreCase(message.chatType())) {
            SubjectEntity groupSubject = subjectService.getOrCreateFeishuGroupSubject(
                    message.chatId(),
                    message.chatName(),
                    orgId
            );
            subjectService.ensureGroupMember(orgId, groupSubject.getId(), userId);
            return groupSubject;
        }
        return subjectService.getOrCreateUserSubject(userId, orgId);
    }
}
