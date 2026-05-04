package org.example.ggbot.adapter.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.adapter.web.dto.WebChatSendRequest;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.service.context.PersistentConversationContextService;
import org.example.ggbot.service.conversation.ConversationService;
import org.example.ggbot.service.dto.ConversationContext;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
import org.example.ggbot.service.organization.OrganizationService;
import org.example.ggbot.service.subject.SubjectService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WebChatControllerTest {

    @Test
    void shouldSendWebMessageInPersonalWorkspaceWhenOrgIdMissing() throws Exception {
        IdentityService identityService = mock(IdentityService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        SubjectService subjectService = mock(SubjectService.class);
        ConversationService conversationService = mock(ConversationService.class);
        PersistentConversationContextService contextService = mock(PersistentConversationContextService.class);
        AgentService agentService = mock(AgentService.class);

        UserEntity user = UserEntity.builder().id(3001L).build();
        OrganizationEntity personalOrg = OrganizationEntity.builder().id(1001L).tenantKey("personal:3001").build();
        SubjectEntity subject = SubjectEntity.builder().id(5001L).build();
        ConversationEntity conversation = ConversationEntity.builder().id(7001L).build();

        when(identityService.getOrCreateUserByWebSession("web-user-key-1"))
                .thenReturn(new ResolvedWebUser(user, personalOrg));
        when(subjectService.getOrCreateUserSubject(3001L, 1001L)).thenReturn(subject);
        when(conversationService.createConversation(1001L, 5001L, "web", "hello", 3001L)).thenReturn(conversation);
        when(contextService.buildContext(1001L, 5001L, 7001L))
                .thenReturn(new ConversationContext(1001L, 5001L, 7001L, List.of(), List.of()));
        when(agentService.replyWithPersistentContext(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("hello")))
                .thenReturn("reply");

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebChatController(
                identityService, organizationService, subjectService, conversationService, contextService, agentService
        )).build();

        mockMvc.perform(post("/api/web/chat/send")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "legacy-session",
                                  "messageContent": "hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orgId").value(1001L))
                .andExpect(jsonPath("$.data.conversationId").value(7001L))
                .andExpect(jsonPath("$.data.reply").value("reply"));
    }
}
