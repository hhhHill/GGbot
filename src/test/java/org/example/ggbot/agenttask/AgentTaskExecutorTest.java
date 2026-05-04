package org.example.ggbot.agenttask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.NodeOutput;
import org.example.ggbot.adapter.feishu.FeishuMessageHandler;
import org.example.ggbot.adapter.feishu.FeishuMessageClient;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentResult;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.graph.AgentGraphProperties;
import org.example.ggbot.agent.graph.GGBotAgentGraphState;
import org.example.ggbot.agent.runner.LangGraphAgentRunner;
import org.example.ggbot.common.IdGenerator;
import org.example.ggbot.memory.ConversationMemoryService;
import org.example.ggbot.memory.SpringAiConversationMemoryService;
import org.example.ggbot.planner.IntentType;
import org.example.ggbot.service.conversation.ConversationService;
import org.example.ggbot.session.WebSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class AgentTaskExecutorTest {

    @Test
    void shouldRetryTaskAndEventuallySucceed() {
        InMemoryAgentTaskService taskService = new InMemoryAgentTaskService(new IdGenerator());
        AgentService agentService = mock(AgentService.class);
        WebSessionService sessionService = mock(WebSessionService.class);
        ConversationService conversationService = mock(ConversationService.class);
        FeishuMessageClient feishuMessageClient = mock(FeishuMessageClient.class);
        AgentTaskExecutor executor = executor(
                taskService, agentService, mock(ConversationMemoryService.class), sessionService, conversationService, feishuMessageClient,
                mock(FeishuMessageHandler.class), mock(LangGraphAgentRunner.class)
        );

        AgentTaskRecord task = taskService.createTask(webRequest("生成文档"), "web", null);
        when(agentService.handle(task.getRequest()))
                .thenThrow(new RuntimeException("first failure"))
                .thenReturn(new AgentResult("task-1", IntentType.CHAT, "最终结果", List.of("artifact-1")));

        executor.process(task.getTaskId());

        AgentTaskRecord updated = taskService.findByTaskId(task.getTaskId());
        assertThat(updated.getStatus()).isEqualTo(AgentTaskStatus.SUCCESS);
        assertThat(updated.getRetryCount()).isEqualTo(1);
        assertThat(updated.getResult()).contains("最终结果");
        verify(agentService, times(2)).handle(task.getRequest());
        verify(sessionService).appendAssistantMessage(eq("user-1"), eq("session-1"), anyString());
        verify(feishuMessageClient, never()).sendText(anyString(), anyString());
    }

    @Test
    void shouldReplyToFeishuAfterSuccess() {
        InMemoryAgentTaskService taskService = new InMemoryAgentTaskService(new IdGenerator());
        AgentService agentService = mock(AgentService.class);
        WebSessionService sessionService = mock(WebSessionService.class);
        ConversationService conversationService = mock(ConversationService.class);
        FeishuMessageClient feishuMessageClient = mock(FeishuMessageClient.class);
        AgentTaskExecutor executor = executor(
                taskService, agentService, mock(ConversationMemoryService.class), sessionService, conversationService, feishuMessageClient,
                mock(FeishuMessageHandler.class), mock(LangGraphAgentRunner.class)
        );

        AgentTaskRecord task = taskService.createTask(feishuRequest("帮我总结"), "feishu", "event-1");
        when(agentService.handle(task.getRequest()))
                .thenReturn(new AgentResult("task-2", IntentType.CHAT, "飞书结果", List.of()));

        executor.process(task.getTaskId());

        verify(feishuMessageClient).sendText("chat-1", "飞书结果");
    }

    @Test
    void shouldMarkTaskFailedAfterMaxRetries() {
        InMemoryAgentTaskService taskService = new InMemoryAgentTaskService(new IdGenerator());
        AgentService agentService = mock(AgentService.class);
        WebSessionService sessionService = mock(WebSessionService.class);
        ConversationService conversationService = mock(ConversationService.class);
        FeishuMessageClient feishuMessageClient = mock(FeishuMessageClient.class);
        AgentTaskExecutor executor = executor(
                taskService, agentService, mock(ConversationMemoryService.class), sessionService, conversationService, feishuMessageClient,
                mock(FeishuMessageHandler.class), mock(LangGraphAgentRunner.class)
        );

        AgentTaskRecord task = taskService.createTask(feishuRequest("帮我总结"), "feishu", "event-1");
        when(agentService.handle(task.getRequest())).thenThrow(new RuntimeException("always fail"));

        executor.process(task.getTaskId());

        AgentTaskRecord updated = taskService.findByTaskId(task.getTaskId());
        assertThat(updated.getStatus()).isEqualTo(AgentTaskStatus.FAILED);
        assertThat(updated.getRetryCount()).isEqualTo(updated.getMaxRetry());
        verify(feishuMessageClient).sendText("chat-1", "任务执行失败，请稍后重试。");
    }

    @Test
    void shouldStreamProgressAndCompleteTask() throws Exception {
        InMemoryAgentTaskService taskService = new InMemoryAgentTaskService(new IdGenerator());
        AgentService agentService = mock(AgentService.class);
        WebSessionService sessionService = mock(WebSessionService.class);
        ConversationService conversationService = mock(ConversationService.class);
        FeishuMessageClient feishuMessageClient = mock(FeishuMessageClient.class);
        FeishuMessageHandler feishuMessageHandler = mock(FeishuMessageHandler.class);
        ConversationMemoryService memoryService = new SpringAiConversationMemoryService(MessageWindowChatMemory.builder().maxMessages(20).build());
        LangGraphAgentRunner runner = mock(LangGraphAgentRunner.class);
        AgentGraphProperties properties = new AgentGraphProperties();
        properties.setMaxIterations(10);
        AgentTaskExecutor executor = new AgentTaskExecutor(
                taskService,
                agentService,
                memoryService,
                sessionService,
                conversationService,
                feishuMessageClient,
                feishuMessageHandler,
                runner,
                properties,
                Runnable::run
        );

        AgentTaskRecord task = taskService.createTask(webRequest("生成文档"), "web", null);
        SseEmitter emitter = mock(SseEmitter.class);
        doNothing().when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        doNothing().when(emitter).complete();

        AgentState finalState = AgentState.initialize(task.getTaskId(), task.getRequest(), List.of());
        finalState.setDone(true);
        finalState.setFinalReply("流式最终结果");
        Map<String, Object> outputInput = GGBotAgentGraphState.inputOf(finalState);
        when(runner.stream(any(AgentState.class), eq(10))).thenReturn(new SingleOutputGenerator(
                new NodeOutput<>("execute", new GGBotAgentGraphState(outputInput)),
                () -> GGBotAgentGraphState.release(outputInput)
        ));

        executor.submitStream(task.getTaskId(), emitter);

        AgentTaskRecord updated = taskService.findByTaskId(task.getTaskId());
        assertThat(updated.getStatus()).isEqualTo(AgentTaskStatus.SUCCESS);
        assertThat(updated.getResult()).contains("流式最终结果");
        assertThat(memoryService.getConversationHistory("session-1")).containsExactly(
                "USER: 生成文档",
                "AGENT: 流式最终结果"
        );
        verify(sessionService).appendAssistantMessage(eq("user-1"), eq("session-1"), eq("流式最终结果"));
        verify(feishuMessageClient, never()).sendText(anyString(), anyString());
        verify(emitter).complete();
    }

    @Test
    void shouldPersistAssistantReplyToConversationWhenOrgMetadataPresent() {
        InMemoryAgentTaskService taskService = new InMemoryAgentTaskService(new IdGenerator());
        AgentService agentService = mock(AgentService.class);
        WebSessionService sessionService = mock(WebSessionService.class);
        ConversationService conversationService = mock(ConversationService.class);
        FeishuMessageClient feishuMessageClient = mock(FeishuMessageClient.class);
        AgentTaskExecutor executor = executor(
                taskService, agentService, mock(ConversationMemoryService.class), sessionService, conversationService, feishuMessageClient,
                mock(FeishuMessageHandler.class), mock(LangGraphAgentRunner.class)
        );

        AgentTaskRecord task = taskService.createTask(persistentWebRequest("生成文档"), "web", null);
        when(agentService.handle(task.getRequest()))
                .thenReturn(new AgentResult("task-3", IntentType.CHAT, "数据库结果", List.of()));

        executor.process(task.getTaskId());

        verify(conversationService).addMessage(1001L, 7001L, null, org.example.ggbot.enums.MessageRole.ASSISTANT, "数据库结果", "text", null);
        verify(sessionService, never()).appendAssistantMessage(anyString(), anyString(), anyString());
    }

    private AgentTaskExecutor executor(
            AgentTaskService taskService,
            AgentService agentService,
            ConversationMemoryService memoryService,
            WebSessionService sessionService,
            ConversationService conversationService,
            FeishuMessageClient feishuMessageClient,
            FeishuMessageHandler feishuMessageHandler,
            LangGraphAgentRunner runner) {
        return new AgentTaskExecutor(
                taskService,
                agentService,
                memoryService,
                sessionService,
                conversationService,
                feishuMessageClient,
                feishuMessageHandler,
                runner,
                new AgentGraphProperties(),
                Runnable::run
        );
    }

    private AgentRequest webRequest(String input) {
        return new AgentRequest("session-1", "user-1", input, AgentChannel.WEB, null, "session-1", Map.of());
    }

    private AgentRequest feishuRequest(String input) {
        return new AgentRequest("chat-1", "user-2", input, AgentChannel.FEISHU, "message-1", "chat-1", Map.of());
    }

    private AgentRequest persistentWebRequest(String input) {
        return new AgentRequest("7001", "3001", input, AgentChannel.WEB, null, "7001", Map.of("orgId", 1001L));
    }

    private static final class SingleOutputGenerator implements AsyncGenerator.Cancellable<NodeOutput<GGBotAgentGraphState>> {

        private final NodeOutput<GGBotAgentGraphState> output;
        private final Runnable cleanup;
        private final AtomicBoolean emitted = new AtomicBoolean(false);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean cleaned = new AtomicBoolean(false);

        private SingleOutputGenerator(NodeOutput<GGBotAgentGraphState> output, Runnable cleanup) {
            this.output = output;
            this.cleanup = cleanup;
        }

        @Override
        public AsyncGenerator.Data<NodeOutput<GGBotAgentGraphState>> next() {
            if (cancelled.get()) {
                cleanup();
                return AsyncGenerator.Data.done();
            }
            if (emitted.compareAndSet(false, true)) {
                return AsyncGenerator.Data.of(output);
            }
            cleanup();
            return AsyncGenerator.Data.done();
        }

        @Override
        public java.util.concurrent.Executor executor() {
            return Runnable::run;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled.set(true);
            cleanup();
            return true;
        }

        private void cleanup() {
            if (cleaned.compareAndSet(false, true)) {
                cleanup.run();
            }
        }
    }
}
