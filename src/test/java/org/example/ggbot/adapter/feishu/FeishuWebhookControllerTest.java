package org.example.ggbot.adapter.feishu;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agenttask.AgentTaskCreationResult;
import org.example.ggbot.agenttask.AgentTaskExecutor;
import org.example.ggbot.agenttask.AgentTaskRecord;
import org.example.ggbot.agenttask.AgentTaskService;
import org.example.ggbot.agenttask.AgentTaskStatus;
import org.example.ggbot.common.JsonUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FeishuWebhookControllerTest {

    @Test
    void shouldReturnChallengeDirectly() throws Exception {
        FeishuEventParser parser = new FeishuEventParser(new JsonUtils(new ObjectMapper()));
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor taskExecutor = mock(AgentTaskExecutor.class);
        FeishuMessageClient messageClient = mock(FeishuMessageClient.class);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new FeishuWebhookController(parser, taskService, taskExecutor, messageClient)).build();

        mockMvc.perform(post("/feishu/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "challenge": "challenge-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").value("challenge-1"));
    }

    @Test
    void shouldHandleMessageReceiveEventWithPersistentFlow() throws Exception {
        FeishuEventParser parser = new FeishuEventParser(new JsonUtils(new ObjectMapper()));
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor taskExecutor = mock(AgentTaskExecutor.class);
        FeishuMessageClient messageClient = mock(FeishuMessageClient.class);
        AgentTaskRecord task = new AgentTaskRecord(
                "agent-task-1",
                "oc_group_1",
                "ou_123",
                "feishu",
                "hello",
                AgentTaskStatus.PENDING,
                null,
                null,
                0,
                3,
                "om_1",
                "oc_group_1",
                "om_1",
                Instant.now(),
                Instant.now(),
                null,
                null,
                new AgentRequest("oc_group_1", "ou_123", "hello", AgentChannel.FEISHU, "om_1", "oc_group_1", Map.of())
        );
        when(taskService.createOrGetByExternalEventId(any(), eq("feishu"), eq("om_1")))
                .thenReturn(new AgentTaskCreationResult(task, true));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new FeishuWebhookController(parser, taskService, taskExecutor, messageClient)).build();

        mockMvc.perform(post("/feishu/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "header": {
                                    "event_type": "im.message.receive_v1"
                                  },
                                  "event": {
                                    "tenant_key": "tenant-1",
                                    "message": {
                                      "chat_id": "oc_group_1",
                                      "chat_type": "group",
                                      "message_id": "om_1",
                                      "content": "{\\\"text\\\":\\\"hello\\\"}"
                                    },
                                    "sender": {
                                      "sender_id": {
                                        "open_id": "ou_123"
                                      }
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value("agent-task-1"))
                .andExpect(jsonPath("$.data.sessionId").value("oc_group_1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void shouldReplyBusyWhenConversationAlreadyHasActiveTask() throws Exception {
        FeishuEventParser parser = new FeishuEventParser(new JsonUtils(new ObjectMapper()));
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor taskExecutor = mock(AgentTaskExecutor.class);
        FeishuMessageClient messageClient = mock(FeishuMessageClient.class);
        AgentTaskRecord activeTask = new AgentTaskRecord(
                "agent-task-1",
                "oc_group_1",
                "ou_123",
                "feishu",
                "hello",
                AgentTaskStatus.RUNNING,
                null,
                null,
                0,
                3,
                "om_1",
                "oc_group_1",
                "om_1",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                null,
                new AgentRequest("oc_group_1", "ou_123", "hello", AgentChannel.FEISHU, "om_1", "oc_group_1", Map.of())
        );
        when(taskService.findActiveTask("feishu", "oc_group_1")).thenReturn(java.util.Optional.of(activeTask));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new FeishuWebhookController(parser, taskService, taskExecutor, messageClient)).build();

        mockMvc.perform(post("/feishu/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "header": {
                                    "event_type": "im.message.receive_v1"
                                  },
                                  "event": {
                                    "tenant_key": "tenant-1",
                                    "message": {
                                      "chat_id": "oc_group_1",
                                      "chat_type": "group",
                                      "message_id": "om_2",
                                      "content": "{\\\"text\\\":\\\"follow up\\\"}"
                                    },
                                    "sender": {
                                      "sender_id": {
                                        "open_id": "ou_123"
                                      }
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value("agent-task-1"))
                .andExpect(jsonPath("$.data.sessionId").value("oc_group_1"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));

        verify(taskService, never()).createOrGetByExternalEventId(any(), eq("feishu"), eq("om_2"));
        verify(taskExecutor, never()).submit(any());
        verify(messageClient).sendText("oc_group_1", "上一条消息仍在处理中，请等待当前任务完成后再发送下一条。");
    }

    @Test
    void shouldTreatDuplicateEventIdAsIdempotentInsteadOfBusy() throws Exception {
        FeishuEventParser parser = new FeishuEventParser(new JsonUtils(new ObjectMapper()));
        AgentTaskService taskService = mock(AgentTaskService.class);
        AgentTaskExecutor taskExecutor = mock(AgentTaskExecutor.class);
        FeishuMessageClient messageClient = mock(FeishuMessageClient.class);
        AgentTaskRecord existingTask = new AgentTaskRecord(
                "agent-task-1",
                "oc_group_1",
                "ou_123",
                "feishu",
                "hello",
                AgentTaskStatus.RUNNING,
                null,
                null,
                0,
                3,
                "om_1",
                "oc_group_1",
                "om_1",
                Instant.now(),
                Instant.now(),
                Instant.now(),
                null,
                new AgentRequest("oc_group_1", "ou_123", "hello", AgentChannel.FEISHU, "om_1", "oc_group_1", Map.of())
        );
        when(taskService.findActiveTask("feishu", "oc_group_1")).thenReturn(java.util.Optional.of(existingTask));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new FeishuWebhookController(parser, taskService, taskExecutor, messageClient)).build();

        mockMvc.perform(post("/feishu/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "header": {
                                    "event_type": "im.message.receive_v1"
                                  },
                                  "event": {
                                    "tenant_key": "tenant-1",
                                    "message": {
                                      "chat_id": "oc_group_1",
                                      "chat_type": "group",
                                      "message_id": "om_1",
                                      "content": "{\\\"text\\\":\\\"hello\\\"}"
                                    },
                                    "sender": {
                                      "sender_id": {
                                        "open_id": "ou_123"
                                      }
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskId").value("agent-task-1"))
                .andExpect(jsonPath("$.data.sessionId").value("oc_group_1"))
                .andExpect(jsonPath("$.data.status").value("RUNNING"));

        verify(messageClient, never()).sendText(any(), any());
    }
}
