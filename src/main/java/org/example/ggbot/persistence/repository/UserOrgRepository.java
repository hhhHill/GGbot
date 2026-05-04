package org.example.ggbot.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.example.ggbot.enums.UserOrgStatus;
import org.example.ggbot.persistence.entity.UserOrgEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOrgRepository extends JpaRepository<UserOrgEntity, Long> {

    boolean existsByUserIdAndOrgIdAndStatus(Long userId, Long orgId, UserOrgStatus status);

    Optional<UserOrgEntity> findByUserIdAndOrgId(Long userId, Long orgId);

    List<UserOrgEntity> findByUserIdAndStatus(Long userId, UserOrgStatus status);
}
