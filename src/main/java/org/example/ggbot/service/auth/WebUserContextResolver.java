package org.example.ggbot.service.auth;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.exception.BadRequestException;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebUserContextResolver {

    private final IdentityService identityService;
    private final LocalAuthService localAuthService;

    public WebUserContext resolve(Cookie authCookie, Cookie webUserKeyCookie, String fallbackWebUserKey, boolean generateIfMissing) {
        String webUserKey = resolveWebUserKey(webUserKeyCookie, fallbackWebUserKey, generateIfMissing);
        var authenticatedUser = localAuthService.findAuthenticatedUser(cookieValue(authCookie));
        if (authenticatedUser.isPresent()) {
            return new WebUserContext(
                    webUserKey,
                    authenticatedUser.get().resolvedUser(),
                    true,
                    authenticatedUser.get().username()
            );
        }
        if (webUserKey == null || webUserKey.isBlank()) {
            throw new BadRequestException("web_user_key cookie or clientKey is required");
        }
        ResolvedWebUser resolvedUser = identityService.getOrCreateUserByWebSession(webUserKey);
        return new WebUserContext(webUserKey, resolvedUser, false, null);
    }

    private String resolveWebUserKey(Cookie cookie, String fallbackWebUserKey, boolean generateIfMissing) {
        String cookieValue = cookieValue(cookie);
        if (cookieValue != null && !cookieValue.isBlank()) {
            return cookieValue;
        }
        if (fallbackWebUserKey != null && !fallbackWebUserKey.isBlank()) {
            return fallbackWebUserKey;
        }
        if (generateIfMissing) {
            return "web-" + UUID.randomUUID();
        }
        return null;
    }

    private String cookieValue(Cookie cookie) {
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }
}
