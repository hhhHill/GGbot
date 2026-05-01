package org.example.ggbot.persistence.repository;

import org.example.ggbot.enums.UserOrgStatus;
import org.example.ggbot.persistence.entity.UserOrgEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserOrgRepository extends JpaRepository<UserOrgEntity, Long> {

    boolean existsByUserIdAndOrgIdAndStatus(Long userId, Long orgId, UserOrgStatus status);
}
