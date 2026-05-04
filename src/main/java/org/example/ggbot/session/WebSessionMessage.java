package org.example.ggbot.session;

public record WebSessionMessage(
        String role,
        String content,
        long timestamp
) {
}
