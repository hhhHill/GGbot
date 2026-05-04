package org.example.ggbot.adapter.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agenttask.AgentTaskExecutor;
import org.example.ggbot.agenttask.AgentTaskRecord;
import org.example.ggbot.agenttask.AgentTaskService;
import org.example.ggbot.agenttask.AgentTaskStatus;
import org.example.ggbot.session.WebSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AgentTaskControllerTest {

    @Test
    void shouldCreateTaskForChatSendRequest() throws Exception {
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor executor = mock(AgentTaskExecutor.class);
        WebSessionService sessionService = mock(WebSessionService.class);
        AgentTaskRecord task = task("agent-task-1", AgentTaskStatus.PENDING, "hello");
        when(taskService.createTask(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("web"), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(task);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ChatController(taskService, executor, sessionService)).build();

        mockMvc.perform(post("/api/chat/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-1",
                                  "userId": "user-1",
                                  "message": "hello"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value("agent-task-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void shouldReturnTaskStatusAndRetryTask() throws Exception {
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor executor = mock(AgentTaskExecutor.class);
        AgentTaskRecord failed = task("agent-task-1", AgentTaskStatus.FAILED, "hello");
        AgentTaskRecord retried = task("agent-task-1", AgentTaskStatus.PENDING, "hello");
        when(taskService.findByTaskId("agent-task-1")).thenReturn(failed);
        when(taskService.retry("agent-task-1")).thenReturn(retried);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AgentTaskController(taskService, executor)).build();

        mockMvc.perform(get("/api/tasks/agent-task-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FAILED"));

        mockMvc.perform(post("/api/tasks/agent-task-1/retry").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    private AgentTaskRecord task(String taskId, AgentTaskStatus status, String input) {
        return new AgentTaskRecord(
                taskId,
                "session-1",
                "user-1",
                "web",
                input,
                status,
                null,
                "boom",
                0,
                3,
                null,
                "session-1",
                null,
                Instant.now(),
                Instant.now(),
                null,
                null,
                new AgentRequest("session-1", "user-1", input, AgentChannel.WEB, null, "session-1", Map.of())
        );
    }
}
