package org.example.ggbot.persistence.repository;

import java.util.List;
import java.util.Optional;
import org.example.ggbot.persistence.entity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    Optional<ConversationEntity> findByIdAndOrgId(Long id, Long orgId);

    List<ConversationEntity> findByOrgIdAndSubjectIdOrderByLastMessageAtDesc(Long orgId, Long subjectId);
}
