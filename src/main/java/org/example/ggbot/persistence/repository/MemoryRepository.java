package org.example.ggbot.persistence.repository;

import java.util.List;
import org.example.ggbot.enums.MemoryScope;
import org.example.ggbot.persistence.entity.MemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryRepository extends JpaRepository<MemoryEntity, Long> {

    List<MemoryEntity> findByOrgIdAndSubjectIdAndScopeOrderByUpdatedAtDesc(
            Long orgId, Long subjectId, MemoryScope scope);
}
