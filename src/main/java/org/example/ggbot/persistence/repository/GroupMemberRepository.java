package org.example.ggbot.persistence.repository;

import java.util.Optional;
import org.example.ggbot.enums.MemberStatus;
import org.example.ggbot.persistence.entity.GroupMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, Long> {

    boolean existsByOrgIdAndGroupSubjectIdAndUserIdAndStatus(
            Long orgId, Long groupSubjectId, Long userId, MemberStatus status);

    Optional<GroupMemberEntity> findByOrgIdAndGroupSubjectIdAndUserId(Long orgId, Long groupSubjectId, Long userId);
}
