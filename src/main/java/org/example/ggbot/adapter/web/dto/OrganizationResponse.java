package org.example.ggbot.adapter.web.dto;

public record OrganizationResponse(
        Long orgId,
        String name,
        String provider,
        String orgType,
        String tenantKey,
        String status
) {
}
