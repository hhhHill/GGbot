package org.example.ggbot.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.ggbot.enums.ProviderType;
import org.example.ggbot.enums.SubjectType;

@Entity
@Table(
        name = "subjects",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_subjects_org_id", columnNames = {"org_id", "id"}),
            @UniqueConstraint(
                    name = "uk_subjects_org_type_provider_ref",
                    columnNames = {"org_id", "type", "provider", "ref_id"}
            )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectEntity {

    @Id
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private SubjectType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private ProviderType provider;

    @Column(name = "ref_id", nullable = false, length = 200)
    private String refId;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
