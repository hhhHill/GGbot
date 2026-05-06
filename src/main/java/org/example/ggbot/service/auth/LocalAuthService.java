package org.example.ggbot.service.auth;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.exception.BadRequestException;
import org.example.ggbot.exception.ForbiddenException;
import org.example.ggbot.persistence.entity.LocalAccountEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.persistence.entity.WebAuthSessionEntity;
import org.example.ggbot.persistence.repository.LocalAccountRepository;
import org.example.ggbot.persistence.repository.UserRepository;
import org.example.ggbot.persistence.repository.WebAuthSessionRepository;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocalAuthService {

    private static final int SESSION_DAYS = 30;

    private final LocalAccountRepository localAccountRepository;
    private final WebAuthSessionRepository webAuthSessionRepository;
    private final UserRepository userRepository;
    private final IdentityService identityService;
    private final PasswordHashService passwordHashService;

    @Transactional
    public AuthenticatedSession register(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        validatePassword(password);
        if (localAccountRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new BadRequestException("用户名已存在");
        }
        ResolvedWebUser resolvedUser = identityService.createLocalUser(normalizedUsername);
        LocalDateTime now = LocalDateTime.now();
        localAccountRepository.save(LocalAccountEntity.builder()
                .userId(resolvedUser.user().getId())
                .username(normalizedUsername)
                .passwordHash(passwordHashService.hash(password))
                .createdAt(now)
                .updatedAt(now)
                .build());
        return createSession(resolvedUser, normalizedUsername, now);
    }

    @Transactional
    public AuthenticatedSession login(String username, String password) {
        String normalizedUsername = normalizeUsername(username);
        LocalAccountEntity account = localAccountRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new ForbiddenException("用户名或密码错误"));
        if (!passwordHashService.matches(password, account.getPasswordHash())) {
            throw new ForbiddenException("用户名或密码错误");
        }
        ResolvedWebUser resolvedUser = identityService.getUserGraph(account.getUserId());
        return createSession(resolvedUser, account.getUsername(), LocalDateTime.now());
    }

    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        webAuthSessionRepository.deleteById(token);
    }

    @Transactional
    public Optional<AuthenticatedWebUser> findAuthenticatedUser(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Optional<WebAuthSessionEntity> existingSession = webAuthSessionRepository.findById(token);
        if (existingSession.isEmpty()) {
            return Optional.empty();
        }
        WebAuthSessionEntity session = existingSession.get();
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            webAuthSessionRepository.delete(session);
            return Optional.empty();
        }
        UserEntity user = userRepository.findById(session.getUserId()).orElse(null);
        if (user == null) {
            webAuthSessionRepository.delete(session);
            return Optional.empty();
        }
        LocalAccountEntity account = localAccountRepository.findById(user.getId()).orElse(null);
        if (account == null) {
            webAuthSessionRepository.delete(session);
            return Optional.empty();
        }
        ResolvedWebUser resolvedUser = identityService.getUserGraph(user.getId());
        return Optional.of(new AuthenticatedWebUser(resolvedUser, account.getUsername()));
    }

    private AuthenticatedSession createSession(ResolvedWebUser resolvedUser, String username, LocalDateTime now) {
        String token = UUID.randomUUID().toString();
        webAuthSessionRepository.save(WebAuthSessionEntity.builder()
                .token(token)
                .userId(resolvedUser.user().getId())
                .expiresAt(now.plusDays(SESSION_DAYS))
                .createdAt(now)
                .updatedAt(now)
                .build());
        return new AuthenticatedSession(token, username, resolvedUser);
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new BadRequestException("用户名不能为空");
        }
        String normalized = username.trim();
        if (normalized.length() < 3 || normalized.length() > 100) {
            throw new BadRequestException("用户名长度需在 3 到 100 之间");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new BadRequestException("密码长度至少为 6 位");
        }
    }

    public record AuthenticatedSession(
            String token,
            String username,
            ResolvedWebUser resolvedUser
    ) {
    }
}
