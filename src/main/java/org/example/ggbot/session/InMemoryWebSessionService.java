package org.example.ggbot.session;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.common.IdGenerator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InMemoryWebSessionService implements WebSessionService {

    private static final String DEFAULT_TITLE = "新对话";

    private final IdGenerator idGenerator;
    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    private long lastTimestamp;

    @Override
    public synchronized WebSession createSession(String userId) {
        long now = nextTimestamp();
        SessionState state = new SessionState(
                idGenerator.nextId("session"),
                userId,
                DEFAULT_TITLE,
                new ArrayList<>(),
                now,
                now
        );
        sessions.put(state.sessionId, state);
        return snapshot(state);
    }

    @Override
    public synchronized List<WebSession> listSessions(String userId) {
        return sessions.values().stream()
                .filter(session -> session.userId.equals(userId))
                .sorted(Comparator.comparingLong((SessionState session) -> session.updatedAt).reversed())
                .map(this::snapshot)
                .toList();
    }

    @Override
    public synchronized WebSession getSession(String userId, String sessionId) {
        return snapshot(requireSession(userId, sessionId));
    }

    @Override
    public synchronized WebSession appendUserMessage(String userId, String sessionId, String content) {
        SessionState session = ensureSession(userId, sessionId);
        long timestamp = nextTimestamp();
        session.messages.add(new WebSessionMessage("user", content, timestamp));
        if (DEFAULT_TITLE.equals(session.title)) {
            session.title = titleFrom(content);
        }
        session.updatedAt = timestamp;
        return snapshot(session);
    }

    @Override
    public synchronized WebSession appendAssistantMessage(String userId, String sessionId, String content) {
        SessionState session = ensureSession(userId, sessionId);
        long timestamp = nextTimestamp();
        session.messages.add(new WebSessionMessage("assistant", content, timestamp));
        session.updatedAt = timestamp;
        return snapshot(session);
    }

    @Override
    public synchronized WebSession updateTitle(String userId, String sessionId, String title) {
        SessionState session = requireSession(userId, sessionId);
        session.title = title == null || title.isBlank() ? DEFAULT_TITLE : title.trim();
        session.updatedAt = nextTimestamp();
        return snapshot(session);
    }

    private SessionState requireSession(String userId, String sessionId) {
        SessionState session = sessions.get(sessionId);
        if (session == null || !session.userId.equals(userId)) {
            throw new IllegalArgumentException("Session not found for user: " + sessionId);
        }
        return session;
    }

    private SessionState ensureSession(String userId, String sessionId) {
        SessionState existing = sessions.get(sessionId);
        if (existing != null) {
            if (!existing.userId.equals(userId)) {
                throw new IllegalArgumentException("Session not found for user: " + sessionId);
            }
            return existing;
        }
        long now = nextTimestamp();
        SessionState created = new SessionState(
                sessionId,
                userId,
                DEFAULT_TITLE,
                new ArrayList<>(),
                now,
                now
        );
        sessions.put(sessionId, created);
        return created;
    }

    private WebSession snapshot(SessionState state) {
        return new WebSession(
                state.sessionId,
                state.userId,
                state.title,
                List.copyOf(state.messages),
                state.createdAt,
                state.updatedAt
        );
    }

    private String titleFrom(String content) {
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_TITLE;
        }
        return trimmed.substring(0, Math.min(20, trimmed.length()));
    }

    private long nextTimestamp() {
        long now = Instant.now().toEpochMilli();
        lastTimestamp = Math.max(now, lastTimestamp + 1);
        return lastTimestamp;
    }

    private static final class SessionState {
        private final String sessionId;
        private final String userId;
        private String title;
        private final List<WebSessionMessage> messages;
        private final long createdAt;
        private long updatedAt;

        private SessionState(
                String sessionId,
                String userId,
                String title,
                List<WebSessionMessage> messages,
                long createdAt,
                long updatedAt
        ) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.title = title;
            this.messages = messages;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }
    }
}
