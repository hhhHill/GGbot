package org.example.ggbot.session;

import java.util.List;

public record WebSession(
        String sessionId,
        String userId,
        String title,
        List<WebSessionMessage> messages,
        long createdAt,
        long updatedAt
) {
}
