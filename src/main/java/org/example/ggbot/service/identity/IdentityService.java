package org.example.ggbot.service.identity;

import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.common.IdGenerator;
import org.example.ggbot.enums.ProviderType;
import org.example.ggbot.enums.SubjectType;
import org.example.ggbot.enums.UserOrgRole;
import org.example.ggbot.enums.UserOrgStatus;
import org.example.ggbot.enums.UserStatus;
import org.example.ggbot.exception.NotFoundException;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.entity.UserEntity;
import org.example.ggbot.persistence.entity.UserIdentityEntity;
import org.example.ggbot.persistence.entity.UserOrgEntity;
import org.example.ggbot.persistence.repository.SubjectRepository;
import org.example.ggbot.persistence.repository.UserIdentityRepository;
import org.example.ggbot.persistence.repository.UserOrgRepository;
import org.example.ggbot.persistence.repository.UserRepository;
import org.example.ggbot.service.dto.ResolvedFeishuUser;
import org.example.ggbot.service.dto.ResolvedWebUser;
import org.example.ggbot.service.organization.OrganizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IdentityService {

    private static final String WEB_GLOBAL_TENANT = "GLOBAL";
    private static final String SUBJECT_ACTIVE = "ACTIVE";

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserOrgRepository userOrgRepository;
    private final SubjectRepository subjectRepository;
    private final OrganizationService organizationService;
    private final IdGenerator idGenerator;

    @Transactional
    public ResolvedWebUser getOrCreateUserByWebSession(String webUserKey) {
        return userIdentityRepository.findByProviderAndTenantKeyAndProviderId(
                        ProviderType.WEB, WEB_GLOBAL_TENANT, webUserKey)
                .map(identity -> ensureExistingWebGraph(identity.getUserId()))
                .orElseGet(() -> createWebGraph(webUserKey));
    }

    @Transactional
    public ResolvedFeishuUser getOrCreateUserByFeishuOpenId(
            String openId,
            String tenantKey,
            String tenantName,
            String nickname,
            String avatarUrl
    ) {
        OrganizationEntity org = organizationService.getOrCreateFeishuOrganization(tenantKey, tenantName);
        Optional<UserIdentityEntity> existingIdentity = userIdentityRepository.findByProviderAndTenantKeyAndProviderId(
                ProviderType.FEISHU, tenantKey, openId);

        if (existingIdentity.isPresent()) {
            UserEntity user = userRepository.findById(existingIdentity.get().getUserId())
                    .orElseThrow(() -> new NotFoundException("User not found for identity"));
            ensureUserOrg(user.getId(), org.getId(), UserOrgRole.MEMBER);
            ensureUserSubject(user.getId(), org.getId());
            return new ResolvedFeishuUser(user, org);
        }

        UserEntity user = createUser(nickname, avatarUrl);
        userIdentityRepository.save(UserIdentityEntity.builder()
                .id(idGenerator.nextLongId())
                .userId(user.getId())
                .orgId(org.getId())
                .provider(ProviderType.FEISHU)
                .providerId(openId)
                .tenantKey(tenantKey)
                .extraInfo(buildFeishuExtraInfo(nickname, avatarUrl))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        ensureUserOrg(user.getId(), org.getId(), UserOrgRole.MEMBER);
        ensureUserSubject(user.getId(), org.getId());
        return new ResolvedFeishuUser(user, org);
    }

    private ResolvedWebUser ensureExistingWebGraph(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found for identity"));
        OrganizationEntity personalOrg = organizationService.getOrCreatePersonalOrganization(userId);
        ensureUserOrg(userId, personalOrg.getId(), UserOrgRole.OWNER);
        ensureUserSubject(userId, personalOrg.getId());
        return new ResolvedWebUser(user, personalOrg);
    }

    private ResolvedWebUser createWebGraph(String webUserKey) {
        UserEntity user = createUser(null, null);
        OrganizationEntity personalOrg = organizationService.getOrCreatePersonalOrganization(user.getId());
        userIdentityRepository.save(UserIdentityEntity.builder()
                .id(idGenerator.nextLongId())
                .userId(user.getId())
                .orgId(null)
                .provider(ProviderType.WEB)
                .providerId(webUserKey)
                .tenantKey(WEB_GLOBAL_TENANT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        ensureUserOrg(user.getId(), personalOrg.getId(), UserOrgRole.OWNER);
        ensureUserSubject(user.getId(), personalOrg.getId());
        return new ResolvedWebUser(user, personalOrg);
    }

    private UserEntity createUser(String nickname, String avatarUrl) {
        UserEntity user = UserEntity.builder()
                .id(idGenerator.nextLongId())
                .nickname(nickname)
                .avatarUrl(avatarUrl)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    private void ensureUserOrg(Long userId, Long orgId, UserOrgRole role) {
        userOrgRepository.findByUserIdAndOrgId(userId, orgId).ifPresentOrElse(existing -> {
            if (existing.getStatus() != UserOrgStatus.ACTIVE) {
                existing.setStatus(UserOrgStatus.ACTIVE);
                existing.setRole(role);
                existing.setLeftAt(null);
                existing.setUpdatedAt(LocalDateTime.now());
                userOrgRepository.save(existing);
            }
        }, () -> userOrgRepository.save(UserOrgEntity.builder()
                .id(idGenerator.nextLongId())
                .userId(userId)
                .orgId(orgId)
                .role(role)
                .status(UserOrgStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    private void ensureUserSubject(Long userId, Long orgId) {
        subjectRepository.findByOrgIdAndTypeAndProviderAndRefId(
                orgId, SubjectType.USER, ProviderType.SYSTEM, String.valueOf(userId)
        ).orElseGet(() -> subjectRepository.save(SubjectEntity.builder()
                .id(idGenerator.nextLongId())
                .orgId(orgId)
                .type(SubjectType.USER)
                .provider(ProviderType.SYSTEM)
                .refId(String.valueOf(userId))
                .name(null)
                .status(SUBJECT_ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build()));
    }

    private String buildFeishuExtraInfo(String nickname, String avatarUrl) {
        return "{\"nickname\":\"" + safe(nickname) + "\",\"avatarUrl\":\"" + safe(avatarUrl) + "\"}";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
