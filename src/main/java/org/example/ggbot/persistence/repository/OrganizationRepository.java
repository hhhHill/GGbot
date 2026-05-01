package org.example.ggbot.persistence.repository;

import java.util.Optional;
import org.example.ggbot.enums.ProviderType;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {

    Optional<OrganizationEntity> findByProviderAndTenantKey(ProviderType provider, String tenantKey);
}
