package org.example.ggbot.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.example.ggbot.common.IdGenerator;
import org.junit.jupiter.api.Test;

class InMemoryWebSessionServiceTest {

    @Test
    void shouldCreateSessionWithDefaultTitleAndNoMessages() {
        InMemoryWebSessionService service = new InMemoryWebSessionService(new IdGenerator());

        WebSession session = service.createSession("user-1");

        assertThat(session.sessionId()).startsWith("session-");
        assertThat(session.userId()).isEqualTo("user-1");
        assertThat(session.title()).isEqualTo("新对话");
        assertThat(session.messages()).isEmpty();
    }

    @Test
    void shouldListOnlySessionsForMatchingUserInMostRecentOrder() {
        InMemoryWebSessionService service = new InMemoryWebSessionService(new IdGenerator());
        WebSession first = service.createSession("user-1");
        WebSession second = service.createSession("user-1");
        service.createSession("user-2");

        service.appendUserMessage("user-1", first.sessionId(), "第一条消息");
        service.appendAssistantMessage("user-1", second.sessionId(), "第二个会话回复");

        assertThat(service.listSessions("user-1"))
                .extracting(WebSession::sessionId)
                .containsExactly(second.sessionId(), first.sessionId());
    }

    @Test
    void shouldGenerateTitleFromFirstUserMessageAndKeepCustomTitle() {
        InMemoryWebSessionService service = new InMemoryWebSessionService(new IdGenerator());
        WebSession session = service.createSession("user-1");

        service.appendUserMessage("user-1", session.sessionId(), "这是第一条用户消息，用来生成标题并且只取前二十个字");

        WebSession createdTitleSession = service.getSession("user-1", session.sessionId());
        assertThat(createdTitleSession.title()).isEqualTo("这是第一条用户消息，用来生成标题并且只取");

        service.updateTitle("user-1", session.sessionId(), "自定义标题");
        service.appendUserMessage("user-1", session.sessionId(), "第二条消息不应覆盖标题");

        WebSession updatedSession = service.getSession("user-1", session.sessionId());
        assertThat(updatedSession.title()).isEqualTo("自定义标题");
        assertThat(updatedSession.messages()).hasSize(2);
    }

    @Test
    void shouldRejectUnknownOrForeignSessionAccess() {
        InMemoryWebSessionService service = new InMemoryWebSessionService(new IdGenerator());
        WebSession session = service.createSession("user-1");

        assertThatThrownBy(() -> service.getSession("user-2", session.sessionId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(session.sessionId());
    }
}
