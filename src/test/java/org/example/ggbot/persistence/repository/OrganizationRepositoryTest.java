package org.example.ggbot.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=always"
})
class OrganizationRepositoryTest {

    @Autowired
    private OrganizationRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndFindOrganizationByProviderAndTenantKey() {
        OrganizationEntity organization = new OrganizationEntity(
                1L,
                "feishu",
                "tenant-demo",
                "Example Tenant",
                "feishu_tenant",
                "active"
        );

        repository.save(organization);

        Optional<OrganizationEntity> result = repository.findByProviderAndTenantKey("feishu", "tenant-demo");

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().getName()).isEqualTo("Example Tenant");
    }

    @Test
    void shouldRejectCrossTenantForeignKeyMismatches() {
        seedTenantGraph();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into conversations (id, org_id, subject_id, title, status) values (?, ?, ?, ?, ?)",
                300L, 2L, 100L, "cross-tenant-conversation", "active"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into group_members (id, org_id, group_subject_id, user_id, status) values (?, ?, ?, ?, ?)",
                301L, 2L, 100L, 1L, "active"))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbcTemplate.update(
                "insert into conversations (id, org_id, subject_id, title, status) values (?, ?, ?, ?, ?)",
                200L, 1L, 100L, "tenant-conversation", "active");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into messages (id, org_id, conversation_id, subject_id, role, content) values (?, ?, ?, ?, ?, ?)",
                302L, 2L, 200L, 100L, "user", "cross-tenant-message"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into memory (id, org_id, subject_id, memory_type, scope, content) values (?, ?, ?, ?, ?, ?)",
                303L, 2L, 100L, "summary", "subject", "cross-tenant-memory"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void seedTenantGraph() {
        jdbcTemplate.update(
                "insert into organizations (id, provider, tenant_key, name, org_type, status) values (?, ?, ?, ?, ?, ?)",
                1L, "feishu", "tenant-a", "Tenant A", "feishu_tenant", "active");
        jdbcTemplate.update(
                "insert into organizations (id, provider, tenant_key, name, org_type, status) values (?, ?, ?, ?, ?, ?)",
                2L, "feishu", "tenant-b", "Tenant B", "feishu_tenant", "active");
        jdbcTemplate.update(
                "insert into users (id, display_name, status) values (?, ?, ?)",
                1L, "User One", "active");
        jdbcTemplate.update(
                "insert into subjects (id, org_id, type, provider, ref_id, name) values (?, ?, ?, ?, ?, ?)",
                100L, 1L, "user", "system", "1", "Tenant A User Subject");
    }

    @Configuration
    @EnableJpaRepositories(considerNestedRepositories = true, basePackageClasses = OrganizationRepositoryTest.class)
    @EntityScan(basePackageClasses = OrganizationEntity.class)
    static class TestConfig {
    }

    @Entity
    @Table(name = "organizations")
    static class OrganizationEntity {

        @Id
        private Long id;

        @Column(nullable = false)
        private String provider;

        @Column(name = "tenant_key", nullable = false)
        private String tenantKey;

        @Column(nullable = false)
        private String name;

        @Column(name = "org_type", nullable = false)
        private String orgType;

        @Column(nullable = false)
        private String status;

        protected OrganizationEntity() {
        }

        OrganizationEntity(Long id, String provider, String tenantKey, String name, String orgType, String status) {
            this.id = id;
            this.provider = provider;
            this.tenantKey = tenantKey;
            this.name = name;
            this.orgType = orgType;
            this.status = status;
        }

        String getName() {
            return name;
        }
    }

    interface OrganizationRepository extends JpaRepository<OrganizationEntity, Long> {
        Optional<OrganizationEntity> findByProviderAndTenantKey(String provider, String tenantKey);
    }
}
