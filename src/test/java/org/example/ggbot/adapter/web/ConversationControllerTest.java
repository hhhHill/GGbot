package org.example.ggbot.adapter.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.util.List;
import org.example.ggbot.adapter.web.dto.RenameConversationRequest;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.example.ggbot.persistence.entity.MessageEntity;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.service.access.AccessControlService;
import org.example.ggbot.service.auth.WebUserContext;
import org.example.ggbot.service.auth.WebUserContextResolver;
import org.example.ggbot.service.conversation.ConversationService;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.organization.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ConversationControllerTest {

    @Test
    void shouldListAccessibleConversations() throws Exception {
        WebUserContextResolver resolver = mock(WebUserContextResolver.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        ConversationService conversationService = mock(ConversationService.class);
        AccessControlService accessControlService = mock(AccessControlService.class);
        when(resolver.resolve(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(baseContext());
        when(conversationService.listAccessibleConversations(3001L, 1001L)).thenReturn(List.of(
                ConversationEntity.builder().id(7001L).orgId(1001L).title("Hello").lastMessageAt(LocalDateTime.of(2026, 5, 1, 12, 0)).build()
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ConversationController(resolver, organizationService, accessControlService, conversationService)
        ).build();

        mockMvc.perform(get("/api/conversations")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .param("orgId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].conversationId").value(7001L));
    }

    @Test
    void shouldListMessagesForConversationAfterAccessCheck() throws Exception {
        WebUserContextResolver resolver = mock(WebUserContextResolver.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        ConversationService conversationService = mock(ConversationService.class);
        AccessControlService accessControlService = mock(AccessControlService.class);
        when(resolver.resolve(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(baseContext());
        when(conversationService.listMessages(1001L, 7001L)).thenReturn(List.of(
                MessageEntity.builder().id(8001L).content("hello").build()
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ConversationController(resolver, organizationService, accessControlService, conversationService)
        ).build();

        mockMvc.perform(get("/api/conversations/7001/messages")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .param("orgId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].messageId").value(8001L))
                .andExpect(jsonPath("$.data[0].content").value("hello"));
    }

    @Test
    void shouldRenameConversationAfterAccessCheck() throws Exception {
        WebUserContextResolver resolver = mock(WebUserContextResolver.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        ConversationService conversationService = mock(ConversationService.class);
        AccessControlService accessControlService = mock(AccessControlService.class);
        when(resolver.resolve(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(baseContext());
        when(conversationService.renameConversation(1001L, 7001L, "新标题"))
                .thenReturn(ConversationEntity.builder().id(7001L).orgId(1001L).title("新标题").build());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ConversationController(resolver, organizationService, accessControlService, conversationService)
        ).build();

        mockMvc.perform(patch("/api/conversations/7001/title")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .param("orgId", "1001")
                        .contentType("application/json")
                        .content("""
                                {"title":"新标题"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.conversationId").value(7001L))
                .andExpect(jsonPath("$.data.title").value("新标题"));
    }

    @Test
    void shouldDeleteConversationAfterAccessCheck() throws Exception {
        WebUserContextResolver resolver = mock(WebUserContextResolver.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        ConversationService conversationService = mock(ConversationService.class);
        AccessControlService accessControlService = mock(AccessControlService.class);
        when(resolver.resolve(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(false)))
                .thenReturn(baseContext());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ConversationController(resolver, organizationService, accessControlService, conversationService)
        ).build();

        mockMvc.perform(delete("/api/conversations/7001")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .param("orgId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(conversationService).deleteConversation(1001L, 7001L);
    }

    private WebUserContext baseContext() {
        return new WebUserContext(
                "web-user-key-1",
                new ResolvedWebUser(UserEntity.builder().id(3001L).build(), OrganizationEntity.builder().id(1001L).build()),
                false,
                null
        );
    }
}
