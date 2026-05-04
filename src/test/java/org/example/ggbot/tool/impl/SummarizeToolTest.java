package org.example.ggbot.tool.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentContext;
import org.example.ggbot.ai.ContextAwareChatService;
import org.example.ggbot.prompt.ClasspathPromptRepository;
import org.example.ggbot.tool.ToolName;
import org.example.ggbot.tool.ToolResult;
import org.junit.jupiter.api.Test;

class SummarizeToolTest {

    @Test
    void shouldDelegateToReliableChatServiceWithSystemPromptWhenAvailable() {
        ContextAwareChatService chatService = mock(ContextAwareChatService.class);
        ClasspathPromptRepository repository = mock(ClasspathPromptRepository.class);
        when(chatService.isAvailable()).thenReturn(true);
        when(repository.load("summarize-system-prompt.txt")).thenReturn(
                """
                你是 GGbot 的 Web 对话助手。
                你的回答应当直接、简洁、可执行。
                如果用户只是普通聊天或提问，请直接回答，不要假装生成文档或 PPT。
                """);
        when(chatService.chat(anyString(), eq("你好，请介绍一下你自己"), eq(contextWithoutStreaming()))).thenReturn("这是模型回复");
        SummarizeTool tool = new SummarizeTool(chatService, repository);

        AgentContext context = contextWithoutStreaming();
        ToolResult result = tool.execute("你好，请介绍一下你自己", context, Map.of());

        assertThat(result.getToolName()).isEqualTo(ToolName.SUMMARIZE);
        assertThat(result.getSummary()).isEqualTo("这是模型回复");
        assertThat(result.getArtifact()).isNull();
        verify(repository).load("summarize-system-prompt.txt");
        verify(chatService).isAvailable();
        verify(chatService).chat(
                """
                你是 GGbot 的 Web 对话助手。
                你的回答应当直接、简洁、可执行。
                如果用户只是普通聊天或提问，请直接回答，不要假装生成文档或 PPT。
                """,
                "你好，请介绍一下你自己",
                context);
        verifyNoMoreInteractions(chatService, repository);
    }

    @Test
    void shouldStreamChunksIntoContextConsumerWhenChatServiceSupportsStreaming() {
        ContextAwareChatService chatService = mock(ContextAwareChatService.class);
        ClasspathPromptRepository repository = mock(ClasspathPromptRepository.class);
        AtomicReference<String> streamed = new AtomicReference<>("");
        AgentContext context = streamingContext(streamed);
        when(chatService.isAvailable()).thenReturn(true);
        when(repository.load("summarize-system-prompt.txt")).thenReturn(
                """
                你是 GGbot 的 Web 对话助手。
                你的回答应当直接、简洁、可执行。
                如果用户只是普通聊天或提问，请直接回答，不要假装生成文档或 PPT。
                """);
        when(chatService.stream(anyString(), eq("你好，请介绍一下你自己"), eq(context))).thenReturn(reactor.core.publisher.Flux.just("你", "好"));
        SummarizeTool tool = new SummarizeTool(chatService, repository);

        ToolResult result = tool.execute("你好，请介绍一下你自己", context, Map.of());

        assertThat(result.getSummary()).isEqualTo("你好");
        assertThat(streamed.get()).isEqualTo("你好");
    }

    @Test
    void shouldNotDuplicateTextReplyIntoArtifactForPlainChatResponses() {
        ContextAwareChatService chatService = mock(ContextAwareChatService.class);
        ClasspathPromptRepository repository = mock(ClasspathPromptRepository.class);
        AgentContext context = contextWithoutStreaming();
        when(chatService.isAvailable()).thenReturn(true);
        when(repository.load("summarize-system-prompt.txt")).thenReturn(
                """
                你是 GGbot 的 Web 对话助手。
                你的回答应当直接、简洁、可执行。
                如果用户只是普通聊天或提问，请直接回答，不要假装生成文档或 PPT。
                """);
        when(chatService.chat(anyString(), eq("飞书是什么"), eq(context))).thenReturn("飞书是企业协作平台。");
        SummarizeTool tool = new SummarizeTool(chatService, repository);

        ToolResult result = tool.execute("飞书是什么", context, Map.of());

        assertThat(result.getSummary()).isEqualTo("飞书是企业协作平台。");
        assertThat(result.getArtifact()).isNull();
    }

    @Test
    void shouldFallbackToTemplateWithoutCallingReliableChatServiceWhenUnavailable() {
        ContextAwareChatService chatService = mock(ContextAwareChatService.class);
        ClasspathPromptRepository repository = mock(ClasspathPromptRepository.class);
        when(chatService.isAvailable()).thenReturn(false);
        SummarizeTool tool = new SummarizeTool(chatService, repository);

        ToolResult result = tool.execute("帮我总结一下", contextWithoutStreaming(), Map.of());

        assertThat(result.getToolName()).isEqualTo(ToolName.SUMMARIZE);
        assertThat(result.getSummary()).contains("已收到你的需求：帮我总结一下");
        verify(chatService).isAvailable();
        verifyNoMoreInteractions(chatService, repository);
    }

    private AgentContext contextWithoutStreaming() {
        return new AgentContext(
                "task-1",
                "conversation-1",
                "user-1",
                AgentChannel.WEB,
                List.of("USER: 你好"),
                Map.of()
        );
    }

    private AgentContext streamingContext(AtomicReference<String> streamed) {
        return new AgentContext(
                "task-1",
                "conversation-1",
                "user-1",
                AgentChannel.WEB,
                List.of("USER: 你好"),
                Map.of("streamChunkConsumer", (java.util.function.Consumer<String>) chunk -> streamed.updateAndGet(value -> value + chunk))
        );
    }
}
