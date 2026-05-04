package org.example.ggbot.adapter.feishu;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.service.context.PersistentConversationContextService;
import org.example.ggbot.service.conversation.ConversationService;
import org.example.ggbot.service.dto.ConversationContext;
import org.example.ggbot.service.dto.ResolvedFeishuUser;
import org.example.ggbot.service.identity.IdentityService;
import org.example.ggbot.service.subject.SubjectService;
import org.junit.jupiter.api.Test;

class FeishuMessageHandlerTest {

    @Test
    void shouldPersistGroupConversationInTenantScopedSubjectAndReplyToChat() {
        IdentityService identityService = mock(IdentityService.class);
        SubjectService subjectService = mock(SubjectService.class);
        ConversationService conversationService = mock(ConversationService.class);
        PersistentConversationContextService contextService = mock(PersistentConversationContextService.class);
        AgentService agentService = mock(AgentService.class);
        FeishuMessageClient messageClient = mock(FeishuMessageClient.class);

        OrganizationEntity organization = OrganizationEntity.builder().id(1001L).build();
        UserEntity user = UserEntity.builder().id(3001L).build();
        SubjectEntity groupSubject = SubjectEntity.builder().id(5001L).build();
        ConversationEntity conversation = ConversationEntity.builder().id(7001L).build();

        when(identityService.getOrCreateUserByFeishuOpenId(
                "ou_123", "tenant-1", "Tenant One", "Alice", "https://avatar.example/alice.png"
        )).thenReturn(new ResolvedFeishuUser(user, organization));
        when(subjectService.getOrCreateFeishuGroupSubject("oc_group_1", "Agent Group", 1001L)).thenReturn(groupSubject);
        when(conversationService.getOrCreateActiveConversation(1001L, 5001L, "feishu", 3001L)).thenReturn(conversation);
        when(contextService.buildContext(1001L, 5001L, 7001L))
                .thenReturn(new ConversationContext(1001L, 5001L, 7001L, List.of(), List.of()));
        when(agentService.replyWithPersistentContext(any(), eq("@bot hello"))).thenReturn("reply");

        FeishuMessageHandler handler = new FeishuMessageHandler(
                identityService, subjectService, conversationService, contextService, agentService, messageClient
        );

        handler.handle(new FeishuInboundMessage(
                "tenant-1",
                "Tenant One",
                "ou_123",
                "oc_group_1",
                "Agent Group",
                "group",
                "om_1",
                "@bot hello",
                "Alice",
                "https://avatar.example/alice.png"
        ));

        verify(subjectService).ensureGroupMember(1001L, 5001L, 3001L);
        verify(conversationService).addMessage(1001L, 7001L, 3001L, org.example.ggbot.enums.MessageRole.USER, "@bot hello", "text", "om_1");
        verify(conversationService).addMessage(1001L, 7001L, null, org.example.ggbot.enums.MessageRole.ASSISTANT, "reply", "text", null);
        verify(messageClient).sendText("oc_group_1", "reply");
    }
}
