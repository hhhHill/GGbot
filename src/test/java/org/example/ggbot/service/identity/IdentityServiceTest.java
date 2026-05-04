package org.example.ggbot.service.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.ggbot.common.IdGenerator;
import org.example.ggbot.enums.OrgStatus;
import org.example.ggbot.enums.OrganizationType;
import org.example.ggbot.enums.ProviderType;
import org.example.ggbot.enums.SubjectType;
import org.example.ggbot.enums.UserOrgRole;
import org.example.ggbot.enums.UserOrgStatus;
import org.example.ggbot.enums.UserStatus;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.persistence.entity.UserIdentityEntity;
import org.example.ggbot.persistence.repository.OrganizationRepository;
import org.example.ggbot.persistence.repository.SubjectRepository;
import org.example.ggbot.persistence.repository.UserIdentityRepository;
import org.example.ggbot.persistence.repository.UserOrgRepository;
import org.example.ggbot.persistence.repository.UserRepository;
import org.example.ggbot.service.dto.ResolvedFeishuUser;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.organization.OrganizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private UserOrgRepository userOrgRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private OrganizationService organizationService;

    @Test
    void shouldCreatePersonalOrganizationForWebIdentity() {
        IdentityService service = new IdentityService(
                userRepository,
                userIdentityRepository,
                userOrgRepository,
                subjectRepository,
                organizationService,
                idGenerator
        );

        when(userIdentityRepository.findByProviderAndTenantKeyAndProviderId(
                ProviderType.WEB, "GLOBAL", "web-user-key-1")).thenReturn(Optional.empty());
        when(idGenerator.nextLongId())
                .thenReturn(1001L, 2001L, 3001L, 4001L);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationRepository.findByProviderAndTenantKey(ProviderType.WEB, "personal:1001"))
                .thenReturn(Optional.empty());
        when(organizationRepository.save(any(OrganizationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userIdentityRepository.save(any(UserIdentityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userOrgRepository.findByUserIdAndOrgId(1001L, 2001L)).thenReturn(Optional.empty());
        when(subjectRepository.findByOrgIdAndTypeAndProviderAndRefId(
                2001L, SubjectType.USER, ProviderType.SYSTEM, "1001")).thenReturn(Optional.empty());
        when(subjectRepository.save(any(SubjectEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResolvedWebUser resolved = service.getOrCreateUserByWebSession("web-user-key-1");

        assertThat(resolved.user().getId()).isEqualTo(1001L);
        assertThat(resolved.user().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(resolved.personalOrg().getId()).isEqualTo(2001L);
        assertThat(resolved.personalOrg().getProvider()).isEqualTo(ProviderType.WEB);
        assertThat(resolved.personalOrg().getOrgType()).isEqualTo(OrganizationType.PERSONAL);
        assertThat(resolved.personalOrg().getTenantKey()).isEqualTo("personal:1001");

        ArgumentCaptor<UserIdentityEntity> identityCaptor = ArgumentCaptor.forClass(UserIdentityEntity.class);
        verify(userIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo(ProviderType.WEB);
        assertThat(identityCaptor.getValue().getTenantKey()).isEqualTo("GLOBAL");
        assertThat(identityCaptor.getValue().getProviderId()).isEqualTo("web-user-key-1");
        assertThat(identityCaptor.getValue().getOrgId()).isNull();

        verify(userOrgRepository).save(any());
        verify(subjectRepository).save(any());
    }

    @Test
    void shouldScopeFeishuIdentityByTenantKey() {
        IdentityService service = new IdentityService(
                userRepository,
                userIdentityRepository,
                userOrgRepository,
                subjectRepository,
                organizationService,
                idGenerator
        );

        when(userIdentityRepository.findByProviderAndTenantKeyAndProviderId(
                ProviderType.FEISHU, "tenant-a", "ou_xxx")).thenReturn(Optional.empty());
        when(organizationRepository.findByProviderAndTenantKey(ProviderType.FEISHU, "tenant-a"))
                .thenReturn(Optional.empty());
        when(idGenerator.nextLongId()).thenReturn(5001L, 6001L, 7001L, 8001L);
        when(organizationRepository.save(any(OrganizationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userIdentityRepository.save(any(UserIdentityEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userOrgRepository.findByUserIdAndOrgId(6001L, 5001L)).thenReturn(Optional.empty());
        when(subjectRepository.findByOrgIdAndTypeAndProviderAndRefId(
                5001L, SubjectType.USER, ProviderType.SYSTEM, "6001")).thenReturn(Optional.empty());
        when(subjectRepository.save(any(SubjectEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResolvedFeishuUser resolved = service.getOrCreateUserByFeishuOpenId(
                "ou_xxx", "tenant-a", "Tenant A", "Alice", "https://img");

        assertThat(resolved.org().getId()).isEqualTo(5001L);
        assertThat(resolved.org().getTenantKey()).isEqualTo("tenant-a");
        assertThat(resolved.user().getId()).isEqualTo(6001L);

        ArgumentCaptor<UserIdentityEntity> identityCaptor = ArgumentCaptor.forClass(UserIdentityEntity.class);
        verify(userIdentityRepository).save(identityCaptor.capture());
        assertThat(identityCaptor.getValue().getProvider()).isEqualTo(ProviderType.FEISHU);
        assertThat(identityCaptor.getValue().getTenantKey()).isEqualTo("tenant-a");
        assertThat(identityCaptor.getValue().getProviderId()).isEqualTo("ou_xxx");
        assertThat(identityCaptor.getValue().getOrgId()).isEqualTo(5001L);
        assertThat(identityCaptor.getValue().getExtraInfo()).contains("Alice");
    }

    @Test
    void shouldEnsurePersonalGraphForExistingWebIdentity() {
        IdentityService service = new IdentityService(
                userRepository,
                userIdentityRepository,
                userOrgRepository,
                subjectRepository,
                organizationService,
                idGenerator
        );

        UserIdentityEntity identity = UserIdentityEntity.builder()
                .id(1L)
                .userId(1001L)
                .provider(ProviderType.WEB)
                .tenantKey("GLOBAL")
                .providerId("web-user-key-1")
                .build();
        UserEntity user = UserEntity.builder()
                .id(1001L)
                .nickname("Existing User")
                .status(UserStatus.ACTIVE)
                .build();
        OrganizationEntity personalOrg = OrganizationEntity.builder()
                .id(2001L)
                .name("Personal Workspace")
                .provider(ProviderType.WEB)
                .orgType(OrganizationType.PERSONAL)
                .tenantKey("personal:1001")
                .status(OrgStatus.ACTIVE)
                .build();

        when(userIdentityRepository.findByProviderAndTenantKeyAndProviderId(
                ProviderType.WEB, "GLOBAL", "web-user-key-1")).thenReturn(Optional.of(identity));
        when(userRepository.findById(1001L)).thenReturn(Optional.of(user));
        when(organizationRepository.findByProviderAndTenantKey(ProviderType.WEB, "personal:1001"))
                .thenReturn(Optional.of(personalOrg));
        when(userOrgRepository.findByUserIdAndOrgId(1001L, 2001L))
                .thenReturn(Optional.of(org.example.ggbot.persistence.entity.UserOrgEntity.builder()
                        .id(9001L)
                        .userId(1001L)
                        .orgId(2001L)
                        .role(UserOrgRole.OWNER)
                        .status(UserOrgStatus.ACTIVE)
                        .build()));
        when(subjectRepository.findByOrgIdAndTypeAndProviderAndRefId(
                2001L, SubjectType.USER, ProviderType.SYSTEM, "1001"))
                .thenReturn(Optional.of(SubjectEntity.builder().id(3001L).build()));

        ResolvedWebUser resolved = service.getOrCreateUserByWebSession("web-user-key-1");

        assertThat(resolved.user().getId()).isEqualTo(1001L);
        assertThat(resolved.personalOrg().getId()).isEqualTo(2001L);
        verify(userOrgRepository, never()).save(any());
        verify(subjectRepository, never()).save(any());
    }
}
