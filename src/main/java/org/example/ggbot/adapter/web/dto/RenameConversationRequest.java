package org.example.ggbot.adapter.web.dto;

import jakarta.validation.constraints.NotBlank;

public record RenameConversationRequest(
        @NotBlank(message = "title is required")
        String title
) {
}
