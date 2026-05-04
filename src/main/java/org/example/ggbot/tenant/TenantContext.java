package org.example.ggbot.tenant;

public record TenantContext(
        Long userId,
        Long orgId,
        String provider,
        String tenantKey
) {
}
