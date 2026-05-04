package org.example.ggbot.persistence.repository;

import java.util.Optional;
import org.example.ggbot.enums.ProviderType;
import org.example.ggbot.enums.SubjectType;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<SubjectEntity, Long> {

    Optional<SubjectEntity> findByOrgIdAndTypeAndProviderAndRefId(
            Long orgId, SubjectType type, ProviderType provider, String refId);

    Optional<SubjectEntity> findByIdAndOrgId(Long id, Long orgId);
}
