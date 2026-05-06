package org.example.ggbot.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.example.ggbot.exception.ForbiddenException;
import org.example.ggbot.persistence.entity.LocalAccountEntity;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.persistence.entity.WebAuthSessionEntity;
import org.example.ggbot.persistence.repository.LocalAccountRepository;
import org.example.ggbot.persistence.repository.UserRepository;
import org.example.ggbot.persistence.repository.WebAuthSessionRepository;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.identity.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalAuthServiceTest {

    @Mock
    private LocalAccountRepository localAccountRepository;

    @Mock
    private WebAuthSessionRepository webAuthSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IdentityService identityService;

    private PasswordHashService passwordHashService;

    @InjectMocks
    private LocalAuthService localAuthService;

    @Captor
    private ArgumentCaptor<LocalAccountEntity> localAccountCaptor;

    @BeforeEach
    void setUp() {
        passwordHashService = new PasswordHashService();
        localAuthService = new LocalAuthService(
                localAccountRepository,
                webAuthSessionRepository,
                userRepository,
                identityService,
                passwordHashService
        );
    }

    @Test
    void shouldRegisterLocalAccountAndCreateSession() {
        ResolvedWebUser resolved = resolvedWebUser(3001L, 1001L, "alice");
        when(localAccountRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(identityService.createLocalUser("alice")).thenReturn(resolved);

        LocalAuthService.AuthenticatedSession session = localAuthService.register("alice", "secret-123");

        assertThat(session.username()).isEqualTo("alice");
        assertThat(session.resolvedUser().user().getId()).isEqualTo(3001L);
        assertThat(session.token()).isNotBlank();
        verify(localAccountRepository).save(localAccountCaptor.capture());
        assertThat(localAccountCaptor.getValue().getUsername()).isEqualTo("alice");
        assertThat(passwordHashService.matches("secret-123", localAccountCaptor.getValue().getPasswordHash())).isTrue();
        verify(webAuthSessionRepository).save(org.mockito.ArgumentMatchers.any(WebAuthSessionEntity.class));
    }

    @Test
    void shouldRejectInvalidPasswordDuringLogin() {
        when(localAccountRepository.findByUsername("alice")).thenReturn(Optional.of(LocalAccountEntity.builder()
                .userId(3001L)
                .username("alice")
                .passwordHash(passwordHashService.hash("secret-123"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));

        assertThatThrownBy(() -> localAuthService.login("alice", "wrong-password"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void shouldResolveAuthenticatedUserFromSessionToken() {
        when(webAuthSessionRepository.findById("token-1")).thenReturn(Optional.of(WebAuthSessionEntity.builder()
                .token("token-1")
                .userId(3001L)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
        when(userRepository.findById(3001L)).thenReturn(Optional.of(UserEntity.builder().id(3001L).nickname("alice").build()));
        when(localAccountRepository.findById(3001L)).thenReturn(Optional.of(LocalAccountEntity.builder()
                .userId(3001L)
                .username("alice")
                .passwordHash("ignored")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
        when(identityService.getUserGraph(3001L)).thenReturn(resolvedWebUser(3001L, 1001L, "alice"));

        Optional<AuthenticatedWebUser> resolved = localAuthService.findAuthenticatedUser("token-1");

        assertThat(resolved).isPresent();
        assertThat(resolved.get().username()).isEqualTo("alice");
        assertThat(resolved.get().resolvedUser().personalOrg().getId()).isEqualTo(1001L);
    }

    private ResolvedWebUser resolvedWebUser(Long userId, Long orgId, String nickname) {
        return new ResolvedWebUser(
                UserEntity.builder().id(userId).nickname(nickname).build(),
                OrganizationEntity.builder().id(orgId).build()
        );
    }
}
