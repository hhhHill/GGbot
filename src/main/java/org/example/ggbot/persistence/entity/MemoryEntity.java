package org.example.ggbot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.ggbot.enums.MemoryScope;
import org.example.ggbot.enums.MemoryType;

@Entity
@Table(name = "memory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryEntity {

    @Id
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false, length = 50)
    private MemoryType memoryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 50)
    private MemoryScope scope;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "embedding_id", length = 200)
    private String embeddingId;

    @Column(name = "source_conversation_id")
    private Long sourceConversationId;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
