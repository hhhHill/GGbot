package org.example.ggbot.persistence.repository;

import java.util.List;
import org.example.ggbot.enums.MemoryScope;
import org.example.ggbot.persistence.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemoryRepository extends JpaRepository<MemoryEntity, Long> {

    List<MemoryEntity> findByOrgIdAndSubjectIdAndScopeOrderByUpdatedAtDesc(
            Long orgId, Long subjectId, MemoryScope scope);

    List<MemoryEntity> findByOrgIdAndSubjectIdOrderByUpdatedAtDesc(Long orgId, Long subjectId);

    @Query("""
            select m from MemoryEntity m
            where m.orgId = :orgId
              and m.subjectId in (
                select s.id from SubjectEntity s
                where s.orgId = :orgId
                  and s.type = org.example.ggbot.enums.SubjectType.USER
                  and s.provider = org.example.ggbot.enums.ProviderType.SYSTEM
                  and s.refId = :userIdRef
                union
                select gm.groupSubjectId from GroupMemberEntity gm
                where gm.orgId = :orgId
                  and gm.userId = :userId
                  and gm.status = org.example.ggbot.enums.MemberStatus.ACTIVE
              )
            order by m.updatedAt desc
            """)
    List<MemoryEntity> findAccessibleMemory(
            @Param("userId") Long userId,
            @Param("userIdRef") String userIdRef,
            @Param("orgId") Long orgId
    );
}
