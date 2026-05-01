package org.example.ggbot.service.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.example.ggbot.common.IdGenerator;
import org.example.ggbot.enums.MemoryScope;
import org.example.ggbot.enums.MemoryType;
import org.example.ggbot.persistence.entity.MemoryEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.example.ggbot.persistence.repository.MemoryRepository;
import org.example.ggbot.persistence.repository.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemoryServiceTest {

    @Mock
    private MemoryRepository memoryRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private IdGenerator idGenerator;

    @InjectMocks
    private MemoryService memoryService;

    @Test
    void shouldPersistMemoryUnderMatchingSubject() {
        when(subjectRepository.findByIdAndOrgId(5001L, 1001L)).thenReturn(Optional.of(
                SubjectEntity.builder().id(5001L).orgId(1001L).build()));
        when(idGenerator.nextLongId()).thenReturn(9001L);
        when(memoryRepository.save(any(MemoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MemoryEntity memory = memoryService.addMemory(
                1001L, 5001L, "remember this", MemoryType.LONG_TERM, MemoryScope.GLOBAL, 7001L, 3001L);

        assertThat(memory.getId()).isEqualTo(9001L);
        assertThat(memory.getOrgId()).isEqualTo(1001L);
        assertThat(memory.getSubjectId()).isEqualTo(5001L);
        assertThat(memory.getMemoryType()).isEqualTo(MemoryType.LONG_TERM);
        assertThat(memory.getScope()).isEqualTo(MemoryScope.GLOBAL);
    }

    @Test
    void shouldListOnlyMemoryVisibleThroughAccessibleSubjects() {
        when(memoryRepository.findAccessibleMemory(3001L, "3001", 1001L))
                .thenReturn(List.of(MemoryEntity.builder().id(8001L).orgId(1001L).content("shared").build()));

        assertThat(memoryService.listAccessibleMemory(3001L, 1001L)).hasSize(1);
    }

    @Test
    void shouldLoadMemoryForSpecificSubject() {
        when(memoryRepository.findByOrgIdAndSubjectIdOrderByUpdatedAtDesc(1001L, 5001L))
                .thenReturn(List.of(MemoryEntity.builder().id(8002L).subjectId(5001L).content("subject-memory").build()));

        List<MemoryEntity> memory = memoryService.listSubjectMemory(1001L, 5001L);

        assertThat(memory).extracting(MemoryEntity::getContent).containsExactly("subject-memory");
        verify(memoryRepository).findByOrgIdAndSubjectIdOrderByUpdatedAtDesc(1001L, 5001L);
    }
}
