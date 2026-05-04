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

@Entity
@Table(
        name = "user_identities",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_identities_provider_tenant_provider_id",
                columnNames = {"provider", "tenant_key", "provider_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserIdentityEntity {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "org_id")
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private ProviderType provider;

    @Column(name = "provider_id", nullable = false, length = 200)
    private String providerId;

    @Column(name = "tenant_key", nullable = false, length = 200)
    private String tenantKey;

    @Column(name = "extra_info", columnDefinition = "text")
    private String extraInfo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
