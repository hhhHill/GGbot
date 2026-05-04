package org.example.ggbot.persistence.repository;

import java.util.List;
import org.example.ggbot.persistence.entity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    List<MessageEntity> findByOrgIdAndConversationIdOrderByCreatedAtAsc(Long orgId, Long conversationId);

    List<MessageEntity> findTop20ByOrgIdAndConversationIdOrderByCreatedAtDesc(Long orgId, Long conversationId);

    void deleteByOrgIdAndConversationId(Long orgId, Long conversationId);
}
