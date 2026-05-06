package org.example.ggbot.service.auth;

import org.example.ggbot.service.dto.ResolvedWebUser;

public record AuthenticatedWebUser(
        ResolvedWebUser resolvedUser,
        String username
) {
}
