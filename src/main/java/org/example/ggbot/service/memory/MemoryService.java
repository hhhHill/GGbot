package org.example.ggbot.service.memory;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.common.IdGenerator;
import org.example.ggbot.enums.MemoryScope;
import org.example.ggbot.enums.MemoryType;
import org.example.ggbot.exception.NotFoundException;
import org.example.ggbot.persistence.entity.MemoryEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.repository.MemoryRepository;
import org.example.ggbot.persistence.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final SubjectRepository subjectRepository;
    private final IdGenerator idGenerator;

    @Transactional
    public MemoryEntity addMemory(
            Long orgId,
            Long subjectId,
            String content,
            MemoryType memoryType,
            MemoryScope scope,
            Long conversationId,
            Long createdByUserId
    ) {
        SubjectEntity subject = subjectRepository.findByIdAndOrgId(subjectId, orgId)
                .orElseThrow(() -> new NotFoundException("Subject not found"));
        MemoryEntity memory = MemoryEntity.builder()
                .id(idGenerator.nextLongId())
                .orgId(orgId)
                .subjectId(subject.getId())
                .content(content)
                .memoryType(memoryType)
                .scope(scope)
                .sourceConversationId(conversationId)
                .createdByUserId(createdByUserId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return memoryRepository.save(memory);
    }

    public List<MemoryEntity> listAccessibleMemory(Long userId, Long orgId) {
        return memoryRepository.findAccessibleMemory(userId, String.valueOf(userId), orgId);
    }

    public List<MemoryEntity> listSubjectMemory(Long orgId, Long subjectId) {
        return memoryRepository.findByOrgIdAndSubjectIdOrderByUpdatedAtDesc(orgId, subjectId);
    }
}
