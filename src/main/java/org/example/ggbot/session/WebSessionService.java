package org.example.ggbot.session;

import java.util.List;

public interface WebSessionService {

    WebSession createSession(String userId);

    List<WebSession> listSessions(String userId);

    WebSession getSession(String userId, String sessionId);

    WebSession appendUserMessage(String userId, String sessionId, String content);

    WebSession appendAssistantMessage(String userId, String sessionId, String content);

    WebSession updateTitle(String userId, String sessionId, String title);
}
