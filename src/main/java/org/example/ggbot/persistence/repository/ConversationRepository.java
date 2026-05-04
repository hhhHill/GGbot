package org.example.ggbot.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    Optional<ConversationEntity> findByIdAndOrgId(Long id, Long orgId);

    List<ConversationEntity> findByOrgIdAndSubjectIdOrderByLastMessageAtDesc(Long orgId, Long subjectId);

    @Query("""
            select c from ConversationEntity c
            where c.orgId = :orgId
              and c.subjectId in (
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
            order by c.lastMessageAt desc
            """)
    List<ConversationEntity> findAccessibleConversations(
            @Param("userId") Long userId,
            @Param("userIdRef") String userIdRef,
            @Param("orgId") Long orgId
    );
}
