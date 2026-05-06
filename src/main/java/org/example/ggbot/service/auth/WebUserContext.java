package org.example.ggbot.service.auth;

import org.example.ggbot.service.dto.ResolvedWebUser;

public record WebUserContext(
        String webUserKey,
        ResolvedWebUser resolvedUser,
        boolean authenticated,
        String loginName
) {
}
