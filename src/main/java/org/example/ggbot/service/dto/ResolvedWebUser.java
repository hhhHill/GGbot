package org.example.ggbot.service.dto;

import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.UserEntity;

public record ResolvedWebUser(
        UserEntity user,
        OrganizationEntity personalOrg
) {
}
