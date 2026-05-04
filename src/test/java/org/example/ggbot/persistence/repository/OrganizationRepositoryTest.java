package org.example.ggbot.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import org.example.ggbot.enums.MessageRole;
import org.example.ggbot.enums.OrgStatus;
import org.example.ggbot.enums.OrganizationType;
import org.example.ggbot.enums.ProviderType;
import org.example.ggbot.enums.SubjectType;
import org.example.ggbot.persistence.entity.MessageEntity;
import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.persistence.entity.SubjectEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always"
})
class OrganizationRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndFindOrganizationByProviderAndTenantKey() {
        OrganizationEntity organization = OrganizationEntity.builder()
                .id(1L)
                .name("Example Tenant")
                .provider(ProviderType.FEISHU)
                .orgType(OrganizationType.FEISHU_TENANT)
                .tenantKey("tenant-demo")
                .status(OrgStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        organizationRepository.save(organization);

        assertThat(organizationRepository.findByProviderAndTenantKey(ProviderType.FEISHU, "tenant-demo"))
                .isPresent()
                .get()
                .extracting(OrganizationEntity::getName)
                .isEqualTo("Example Tenant");
    }

    @Test
    void shouldFindUserSubjectByOrgAndRefId() {
        seedSubjectGraph();

        assertThat(subjectRepository.findByOrgIdAndTypeAndProviderAndRefId(
                10L, SubjectType.USER, ProviderType.SYSTEM, "3001"))
                .isPresent()
                .get()
                .extracting(SubjectEntity::getName)
                .isEqualTo("User Subject");
    }

    @Test
    void shouldListMessagesByOrgAndConversationOrderedByCreatedAt() {
        seedMessageGraph();

        List<MessageEntity> messages = messageRepository.findByOrgIdAndConversationIdOrderByCreatedAtAsc(10L, 30L);

        assertThat(messages)
                .extracting(MessageEntity::getContent)
                .containsExactly("first", "second");
    }

    @Test
    void shouldRejectCrossTenantForeignKeyMismatches() {
        seedSubjectGraph();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into conversations (id, org_id, subject_id, title, source, status) values (?, ?, ?, ?, ?, ?)",
                31L, 11L, 20L, "cross-tenant-conversation", "web", "ACTIVE"))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update(
                "insert into conversations (id, org_id, subject_id, title, source, status) values (?, ?, ?, ?, ?, ?)",
                30L, 10L, 20L, "valid-conversation", "web", "ACTIVE");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into messages (id, org_id, conversation_id, role, content, message_type) values (?, ?, ?, ?, ?, ?)",
                40L, 11L, 30L, "USER", "cross-tenant-message", "text"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void seedSubjectGraph() {
        jdbcTemplate.update(
                "insert into organizations (id, name, provider, org_type, tenant_key, status) values (?, ?, ?, ?, ?, ?)",
                10L, "Tenant A", "FEISHU", "FEISHU_TENANT", "tenant-a", "ACTIVE");
        jdbcTemplate.update(
                "insert into organizations (id, name, provider, org_type, tenant_key, status) values (?, ?, ?, ?, ?, ?)",
                11L, "Tenant B", "FEISHU", "FEISHU_TENANT", "tenant-b", "ACTIVE");
        jdbcTemplate.update(
                "insert into users (id, nickname, status) values (?, ?, ?)",
                3001L, "User One", "ACTIVE");
        jdbcTemplate.update(
                "insert into subjects (id, org_id, type, provider, ref_id, name, status) values (?, ?, ?, ?, ?, ?, ?)",
                20L, 10L, "USER", "SYSTEM", "3001", "User Subject", "active".toUpperCase());
    }

    private void seedMessageGraph() {
        jdbcTemplate.update(
                "insert into organizations (id, name, provider, org_type, tenant_key, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                10L, "Tenant A", "FEISHU", "FEISHU_TENANT", "tenant-a", "ACTIVE",
                LocalDateTime.of(2026, 5, 1, 12, 0), LocalDateTime.of(2026, 5, 1, 12, 0));
        jdbcTemplate.update(
                "insert into users (id, nickname, status, created_at, updated_at) values (?, ?, ?, ?, ?)",
                3001L, "User One", "ACTIVE",
                LocalDateTime.of(2026, 5, 1, 12, 0), LocalDateTime.of(2026, 5, 1, 12, 0));
        jdbcTemplate.update(
                "insert into subjects (id, org_id, type, provider, ref_id, name, status, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                20L, 10L, "USER", "SYSTEM", "3001", "User Subject", "ACTIVE",
                LocalDateTime.of(2026, 5, 1, 12, 0), LocalDateTime.of(2026, 5, 1, 12, 0));
        jdbcTemplate.update(
                "insert into conversations (id, org_id, subject_id, title, source, status, created_by_user_id, last_message_at, created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                30L, 10L, 20L, "Conversation", "web", "ACTIVE", 3001L,
                LocalDateTime.of(2026, 5, 1, 12, 5), LocalDateTime.of(2026, 5, 1, 12, 0),
                LocalDateTime.of(2026, 5, 1, 12, 5));

        jdbcTemplate.update(
                "insert into messages (id, org_id, conversation_id, sender_user_id, role, content, message_type, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                40L, 10L, 30L, 3001L, MessageRole.USER.name(), "first", "text", LocalDateTime.of(2026, 5, 1, 12, 1));
        jdbcTemplate.update(
                "insert into messages (id, org_id, conversation_id, sender_user_id, role, content, message_type, created_at) values (?, ?, ?, ?, ?, ?, ?, ?)",
                41L, 10L, 30L, null, MessageRole.ASSISTANT.name(), "second", "text", LocalDateTime.of(2026, 5, 1, 12, 2));
    }
}
