package org.example.ggbot.service.organization;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.common.IdGenerator;
import org.example.ggbot.enums.OrgStatus;
import org.example.ggbot.enums.OrganizationType;
import org.example.ggbot.enums.ProviderType;
import org.example.ggbot.enums.UserOrgStatus;
import org.example.ggbot.exception.ForbiddenException;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.repository.OrganizationRepository;
import org.example.ggbot.persistence.repository.UserOrgRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserOrgRepository userOrgRepository;
    private final IdGenerator idGenerator;

    @Transactional
    public OrganizationEntity getOrCreateFeishuOrganization(String tenantKey, String tenantName) {
        return organizationRepository.findByProviderAndTenantKey(ProviderType.FEISHU, tenantKey)
                .orElseGet(() -> organizationRepository.save(OrganizationEntity.builder()
                        .id(idGenerator.nextLongId())
                        .name(tenantName == null || tenantName.isBlank() ? tenantKey : tenantName)
                        .provider(ProviderType.FEISHU)
                        .orgType(OrganizationType.FEISHU_TENANT)
                        .tenantKey(tenantKey)
                        .status(OrgStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    @Transactional
    public OrganizationEntity getOrCreatePersonalOrganization(Long userId) {
        String tenantKey = "personal:" + userId;
        return organizationRepository.findByProviderAndTenantKey(ProviderType.WEB, tenantKey)
                .orElseGet(() -> organizationRepository.save(OrganizationEntity.builder()
                        .id(idGenerator.nextLongId())
                        .name("Personal Workspace")
                        .provider(ProviderType.WEB)
                        .orgType(OrganizationType.PERSONAL)
                        .tenantKey(tenantKey)
                        .status(OrgStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    public void checkUserActiveInOrg(Long userId, Long orgId) {
        if (!userOrgRepository.existsByUserIdAndOrgIdAndStatus(userId, orgId, UserOrgStatus.ACTIVE)) {
            throw new ForbiddenException("User is not active in organization");
        }
    }

    public List<OrganizationEntity> listActiveOrganizations(Long userId) {
        return userOrgRepository.findByUserIdAndStatus(userId, UserOrgStatus.ACTIVE).stream()
                .map(userOrg -> organizationRepository.findById(userOrg.getOrgId()).orElse(null))
                .filter(org -> org != null)
                .sorted(Comparator.comparing(OrganizationEntity::getId))
                .toList();
    }
}
