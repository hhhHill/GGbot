package org.example.ggbot.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.adapter.web.dto.AgentTaskAcceptedResponse;
import org.example.ggbot.agenttask.AgentTaskExecutor;
import org.example.ggbot.agenttask.AgentTaskRecord;
import org.example.ggbot.agenttask.AgentTaskService;
import org.example.ggbot.agenttask.AgentTaskStatus;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.service.conversation.ConversationService;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
import org.example.ggbot.service.organization.OrganizationService;
import org.example.ggbot.service.subject.SubjectService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WebAgentControllerTest {

    @Test
    void shouldCreatePersistentConversationWhenConversationIdMissing() throws Exception {
        IdentityService identityService = mock(IdentityService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        SubjectService subjectService = mock(SubjectService.class);
        ConversationService conversationService = mock(ConversationService.class);
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor taskExecutor = mock(AgentTaskExecutor.class);

        UserEntity user = UserEntity.builder().id(3001L).build();
        OrganizationEntity personalOrg = OrganizationEntity.builder().id(1001L).build();
        SubjectEntity subject = SubjectEntity.builder().id(5001L).build();
        ConversationEntity conversation = ConversationEntity.builder().id(7001L).orgId(1001L).subjectId(5001L).build();
        AgentTaskRecord task = taskRecord("task-1", "7001", "3001", "hello");

        when(identityService.getOrCreateUserByWebSession("web-user-key-1"))
                .thenReturn(new ResolvedWebUser(user, personalOrg));
        when(subjectService.getOrCreateUserSubject(3001L, 1001L)).thenReturn(subject);
        when(conversationService.createConversation(1001L, 5001L, "web", "hello", 3001L)).thenReturn(conversation);
        when(taskService.createTask(any(AgentRequest.class), eq("web"), eq(null))).thenReturn(task);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAgentController(
                taskService, taskExecutor, identityService, organizationService, subjectService, conversationService
        )).build();

        mockMvc.perform(post("/api/agent/chat")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value("task-1"))
                .andExpect(jsonPath("$.data.sessionId").value("7001"));

        verify(conversationService).createConversation(1001L, 5001L, "web", "hello", 3001L);
        verify(conversationService).addMessage(1001L, 7001L, 3001L, org.example.ggbot.enums.MessageRole.USER, "hello", "text", null);
        verify(taskExecutor).submit("task-1");
    }

    @Test
    void shouldReusePersistentConversationWhenConversationIdProvided() throws Exception {
        IdentityService identityService = mock(IdentityService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        SubjectService subjectService = mock(SubjectService.class);
        ConversationService conversationService = mock(ConversationService.class);
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor taskExecutor = mock(AgentTaskExecutor.class);

        UserEntity user = UserEntity.builder().id(3001L).build();
        OrganizationEntity personalOrg = OrganizationEntity.builder().id(1001L).build();
        SubjectEntity subject = SubjectEntity.builder().id(5001L).build();
        AgentTaskRecord task = taskRecord("task-2", "7001", "3001", "again");

        when(identityService.getOrCreateUserByWebSession("web-user-key-1"))
                .thenReturn(new ResolvedWebUser(user, personalOrg));
        when(subjectService.getOrCreateUserSubject(3001L, 1001L)).thenReturn(subject);
        when(taskService.createTask(any(AgentRequest.class), eq("web"), eq(null))).thenReturn(task);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAgentController(
                taskService, taskExecutor, identityService, organizationService, subjectService, conversationService
        )).build();

        mockMvc.perform(post("/api/agent/chat")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "7001",
                                  "message": "again"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskId").value("task-2"))
                .andExpect(jsonPath("$.data.sessionId").value("7001"));

        verify(conversationService).addMessage(1001L, 7001L, 3001L, org.example.ggbot.enums.MessageRole.USER, "again", "text", null);
        verify(taskExecutor).submit("task-2");
    }

    private AgentTaskRecord taskRecord(String taskId, String conversationId, String userId, String input) {
        AgentRequest request = new AgentRequest(conversationId, userId, input, AgentChannel.WEB, null, conversationId, Map.of("orgId", 1001L));
        return new AgentTaskRecord(
                taskId,
                conversationId,
                userId,
                "web",
                input,
                AgentTaskStatus.PENDING,
                null,
                null,
                0,
                2,
                null,
                null,
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                request
        );
    }
}
