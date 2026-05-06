package org.example.ggbot.adapter.web.dto;

public record WebContextResponse(
        String webUserKey,
        Long userId,
        Long personalOrgId,
        boolean authenticated,
        String loginName
) {
}
