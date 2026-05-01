package org.example.ggbot.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
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
