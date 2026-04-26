package org.example.ggbot.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ReliableChatServiceTest {

    private static final String FALLBACK_REPLY = "抱歉，我暂时无法处理你的请求，请稍后再试。";

    @Test
    void shouldRetryAfterInitialFailureAndReturnLaterSuccessfulReply() {
        SpringAiChatService delegate = mock(SpringAiChatService.class);
        when(delegate.chat("system", "user"))
                .thenThrow(new RuntimeException("first failure"))
                .thenReturn("recovered reply");

        ReliableChatService service = new ReliableChatService(delegate, ChatFallbackPolicy.createDefault());

        String reply = service.chat("system", "user");

        assertThat(reply).isEqualTo("recovered reply");
        verify(delegate, times(2)).chat("system", "user");
    }

    @Test
    void shouldReturnGenericFallbackWhenAllConfiguredAttemptsProduceBlankReplies() {
        SpringAiChatService delegate = mock(SpringAiChatService.class);
        when(delegate.chat("system", "user"))
                .thenReturn(" ")
                .thenReturn("")
                .thenReturn(null)
                .thenReturn("   ");

        ChatFallbackPolicy fallbackPolicy = ChatFallbackPolicy.createDefault();
        ReliableChatService service = new ReliableChatService(delegate, fallbackPolicy);

        String reply = service.chat("system", "user");

        assertThat(reply).isEqualTo(FALLBACK_REPLY);
        verify(delegate, times(fallbackPolicy.maxAttempts())).chat("system", "user");
    }

    @Test
    void shouldReturnGenericFallbackWhenAllConfiguredAttemptsThrowExceptions() {
        SpringAiChatService delegate = mock(SpringAiChatService.class);
        when(delegate.chat("system", "user"))
                .thenThrow(new RuntimeException("failure-1"))
                .thenThrow(new RuntimeException("failure-2"))
                .thenThrow(new RuntimeException("failure-3"))
                .thenThrow(new RuntimeException("failure-4"));

        ChatFallbackPolicy fallbackPolicy = ChatFallbackPolicy.createDefault();
        ReliableChatService service = new ReliableChatService(delegate, fallbackPolicy);

        String reply = service.chat("system", "user");

        assertThat(reply).isEqualTo(FALLBACK_REPLY);
        verify(delegate, times(fallbackPolicy.maxAttempts())).chat("system", "user");
    }

    @Test
    void shouldLogWarnOnRetryAndErrorWithLastExceptionContextAfterAllAttemptsFail() {
        SpringAiChatService delegate = mock(SpringAiChatService.class);
        RuntimeException firstFailure = new RuntimeException("failure-1");
        RuntimeException lastFailure = new RuntimeException("failure-4");
        when(delegate.chat("system", "user"))
                .thenThrow(firstFailure)
                .thenThrow(new RuntimeException("failure-2"))
                .thenThrow(new RuntimeException("failure-3"))
                .thenThrow(lastFailure);

        Logger logger = (Logger) LoggerFactory.getLogger(ReliableChatService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            ReliableChatService service = new ReliableChatService(delegate, ChatFallbackPolicy.createDefault());

            String reply = service.chat("system", "user");

            assertThat(reply).isEqualTo(FALLBACK_REPLY);
            assertThat(appender.list)
                    .extracting(ILoggingEvent::getLevel)
                    .contains(Level.WARN, Level.ERROR);
            assertThat(appender.list)
                    .filteredOn(event -> event.getLevel() == Level.ERROR)
                    .singleElement()
                    .satisfies(event -> {
                        assertThat(event.getFormattedMessage()).contains("All chat attempts failed");
                        assertThat(event.getThrowableProxy()).isNotNull();
                        assertThat(event.getThrowableProxy().getMessage()).isEqualTo(lastFailure.getMessage());
                    });
        } finally {
            logger.detachAppender(appender);
        }
    }
}
