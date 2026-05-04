package org.example.ggbot.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import org.assertj.core.api.Assertions;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agenttask.AgentTaskExecutor;
import org.example.ggbot.agenttask.AgentTaskRecord;
import org.example.ggbot.agenttask.AgentTaskService;
import org.example.ggbot.agenttask.AgentTaskStatus;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class WebJobControllerTest {

    @Test
    void shouldCreateTaskAndReturnPendingStatusForChatRequests() throws Exception {
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor taskExecutor = mock(AgentTaskExecutor.class);
        IdentityService identityService = mock(IdentityService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        SubjectService subjectService = mock(SubjectService.class);
        ConversationService conversationService = mock(ConversationService.class);
        AgentTaskRecord task = task("agent-task-1", AgentTaskStatus.PENDING);
        when(taskService.createTask(any(), eq("web"), eq(null))).thenReturn(task);
        when(identityService.getOrCreateUserByWebSession("web-user-key-1"))
                .thenReturn(new ResolvedWebUser(UserEntity.builder().id(3001L).build(), OrganizationEntity.builder().id(1001L).build()));
        when(subjectService.getOrCreateUserSubject(3001L, 1001L)).thenReturn(SubjectEntity.builder().id(5001L).build());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAgentController(
                taskService,
                taskExecutor,
                identityService,
                organizationService,
                subjectService,
                conversationService
        )).build();

        mockMvc.perform(post("/api/agent/chat")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "conversationId": "1",
                                  "userId": "user-1",
                                  "message": "你好"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value("agent-task-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(taskExecutor).submit("agent-task-1");
    }

    @Test
    void shouldExposeTaskStatusForLegacyJobEndpoint() throws Exception {
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor taskExecutor = mock(AgentTaskExecutor.class);
        AgentTaskRecord running = task("agent-task-1", AgentTaskStatus.RUNNING);
        when(taskService.findByTaskId("agent-task-1")).thenReturn(running);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAgentController(
                taskService,
                taskExecutor,
                mock(IdentityService.class),
                mock(OrganizationService.class),
                mock(SubjectService.class),
                mock(ConversationService.class)
        )).build();

        mockMvc.perform(get("/api/agent/jobs/agent-task-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value("agent-task-1"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));
    }

    @Test
    void shouldRetryFailedTaskThroughLegacyJobRetryEndpoint() throws Exception {
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor taskExecutor = mock(AgentTaskExecutor.class);
        AgentTaskRecord retried = task("agent-task-1", AgentTaskStatus.PENDING);
        when(taskService.retry("agent-task-1")).thenReturn(retried);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAgentController(
                taskService,
                taskExecutor,
                mock(IdentityService.class),
                mock(OrganizationService.class),
                mock(SubjectService.class),
                mock(ConversationService.class)
        )).build();

        mockMvc.perform(post("/api/agent/jobs/agent-task-1/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value("agent-task-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(taskExecutor).submit("agent-task-1");
    }

    @Test
    void shouldStartStreamEndpointWithSseEmitter() throws Exception {
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor taskExecutor = mock(AgentTaskExecutor.class);
        IdentityService identityService = mock(IdentityService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        SubjectService subjectService = mock(SubjectService.class);
        ConversationService conversationService = mock(ConversationService.class);
        AgentTaskRecord task = task("agent-task-1", AgentTaskStatus.PENDING);
        when(taskService.createTask(any(), eq("web"), eq(null))).thenReturn(task);
        when(identityService.getOrCreateUserByWebSession("web-user-key-1"))
                .thenReturn(new ResolvedWebUser(UserEntity.builder().id(3001L).build(), OrganizationEntity.builder().id(1001L).build()));
        when(subjectService.getOrCreateUserSubject(3001L, 1001L)).thenReturn(SubjectEntity.builder().id(5001L).build());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAgentController(
                taskService,
                taskExecutor,
                identityService,
                organizationService,
                subjectService,
                conversationService
        )).build();

        mockMvc.perform(get("/api/agent/chat/stream")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .param("conversationId", "1")
                        .param("userId", "user-1")
                        .param("message", "你好"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(conversationService).addMessage(1001L, 1L, 3001L, org.example.ggbot.enums.MessageRole.USER, "你好", "text", null);
        verify(taskExecutor).submitStream(eq("agent-task-1"), org.mockito.ArgumentMatchers.any(SseEmitter.class));
    }

    @Test
    void shouldAttachStreamChunkConsumerToStreamRequestMetadata() throws Exception {
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor taskExecutor = mock(AgentTaskExecutor.class);
        IdentityService identityService = mock(IdentityService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        SubjectService subjectService = mock(SubjectService.class);
        ConversationService conversationService = mock(ConversationService.class);
        AgentTaskRecord task = task("agent-task-1", AgentTaskStatus.PENDING);
        when(taskService.createTask(any(), eq("web"), eq(null))).thenReturn(task);
        when(identityService.getOrCreateUserByWebSession("web-user-key-1"))
                .thenReturn(new ResolvedWebUser(UserEntity.builder().id(3001L).build(), OrganizationEntity.builder().id(1001L).build()));
        when(subjectService.getOrCreateUserSubject(3001L, 1001L)).thenReturn(SubjectEntity.builder().id(5001L).build());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WebAgentController(
                taskService,
                taskExecutor,
                identityService,
                organizationService,
                subjectService,
                conversationService
        )).build();

        mockMvc.perform(get("/api/agent/chat/stream")
                        .cookie(new Cookie("web_user_key", "web-user-key-1"))
                        .param("conversationId", "1")
                        .param("userId", "user-1")
                        .param("message", "你好"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(AgentRequest.class);
        verify(taskService).createTask(requestCaptor.capture(), eq("web"), eq(null));
        Object sink = requestCaptor.getValue().getMetadata().get("streamChunkConsumer");
        Assertions.assertThat(sink).isInstanceOf(Consumer.class);
        verify(conversationService).addMessage(1001L, 1L, 3001L, org.example.ggbot.enums.MessageRole.USER, "你好", "text", null);
    }

    private AgentTaskRecord task(String taskId, AgentTaskStatus status) {
        return new AgentTaskRecord(
                taskId,
                "1",
                "3001",
                "web",
                "导出聊天记录",
                status,
                null,
                null,
                0,
                3,
                null,
                "conversation-1",
                null,
                Instant.parse("2026-04-26T12:00:00Z"),
                Instant.parse("2026-04-26T12:00:00Z"),
                null,
                null,
                new AgentRequest("1", "3001", "导出聊天记录", AgentChannel.WEB, null, "1", Map.of("orgId", 1001L))
        );
    }
}
