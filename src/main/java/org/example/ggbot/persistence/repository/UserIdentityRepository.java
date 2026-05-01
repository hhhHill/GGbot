package org.example.ggbot.persistence.repository;

import java.util.Optional;
import org.example.ggbot.enums.ProviderType;
import org.example.ggbot.persistence.entity.UserIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityRepository extends JpaRepository<UserIdentityEntity, Long> {

    Optional<UserIdentityEntity> findByProviderAndTenantKeyAndProviderId(
            ProviderType provider, String tenantKey, String providerId);
}
