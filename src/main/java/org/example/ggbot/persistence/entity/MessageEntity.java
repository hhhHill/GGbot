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
import org.example.ggbot.enums.MessageRole;

@Entity
@Table(
        name = "messages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_messages_provider_message_org",
                columnNames = {"provider_message_id", "org_id"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageEntity {

    @Id
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_user_id")
    private Long senderUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "message_type", nullable = false, length = 50)
    private String messageType;

    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
