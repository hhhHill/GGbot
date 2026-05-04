# Multitenant User System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a JPA-backed multitenant user, organization, subject, conversation, message, and memory domain for GGbot, then expose new Web and Feishu APIs that enforce `org_id + subject` scoped access control.

**Architecture:** Add a new persistent collaboration domain beside the existing in-memory session flow. Keep old endpoints intact while new controllers, services, repositories, and database DDL establish the new multitenant source of truth for identity, workspace membership, conversations, messages, and memory. Route new Web and Feishu chat flows through a persistent context assembler so Agent input comes from database state instead of in-memory chat history.

**Tech Stack:** Spring Boot 3.5, Spring Data JPA, Hibernate, MySQL/H2, Spring Web MVC, Spring Data Redis, JUnit 5, Mockito, MockMvc

---

## File Structure

### Create

- `src/main/resources/schema.sql`
- `src/main/java/org/example/ggbot/enums/ProviderType.java`
- `src/main/java/org/example/ggbot/enums/OrganizationType.java`
- `src/main/java/org/example/ggbot/enums/OrgStatus.java`
- `src/main/java/org/example/ggbot/enums/SubjectType.java`
- `src/main/java/org/example/ggbot/enums/UserStatus.java`
- `src/main/java/org/example/ggbot/enums/UserOrgStatus.java`
- `src/main/java/org/example/ggbot/enums/UserOrgRole.java`
- `src/main/java/org/example/ggbot/enums/MemberStatus.java`
- `src/main/java/org/example/ggbot/enums/MessageRole.java`
- `src/main/java/org/example/ggbot/enums/ConversationStatus.java`
- `src/main/java/org/example/ggbot/enums/MemoryType.java`
- `src/main/java/org/example/ggbot/enums/MemoryScope.java`
- `src/main/java/org/example/ggbot/exception/ForbiddenException.java`
- `src/main/java/org/example/ggbot/exception/NotFoundException.java`
- `src/main/java/org/example/ggbot/exception/BadRequestException.java`
- `src/main/java/org/example/ggbot/exception/GlobalExceptionHandler.java`
- `src/main/java/org/example/ggbot/tenant/TenantContext.java`
- `src/main/java/org/example/ggbot/persistence/entity/UserEntity.java`
- `src/main/java/org/example/ggbot/persistence/entity/OrganizationEntity.java`
- `src/main/java/org/example/ggbot/persistence/entity/UserIdentityEntity.java`
- `src/main/java/org/example/ggbot/persistence/entity/UserOrgEntity.java`
- `src/main/java/org/example/ggbot/persistence/entity/SubjectEntity.java`
- `src/main/java/org/example/ggbot/persistence/entity/GroupMemberEntity.java`
- `src/main/java/org/example/ggbot/persistence/entity/ConversationEntity.java`
- `src/main/java/org/example/ggbot/persistence/entity/MessageEntity.java`
- `src/main/java/org/example/ggbot/persistence/entity/MemoryEntity.java`
- `src/main/java/org/example/ggbot/persistence/repository/UserRepository.java`
- `src/main/java/org/example/ggbot/persistence/repository/OrganizationRepository.java`
- `src/main/java/org/example/ggbot/persistence/repository/UserIdentityRepository.java`
- `src/main/java/org/example/ggbot/persistence/repository/UserOrgRepository.java`
- `src/main/java/org/example/ggbot/persistence/repository/SubjectRepository.java`
- `src/main/java/org/example/ggbot/persistence/repository/GroupMemberRepository.java`
- `src/main/java/org/example/ggbot/persistence/repository/ConversationRepository.java`
- `src/main/java/org/example/ggbot/persistence/repository/MessageRepository.java`
- `src/main/java/org/example/ggbot/persistence/repository/MemoryRepository.java`
- `src/main/java/org/example/ggbot/service/organization/OrganizationService.java`
- `src/main/java/org/example/ggbot/service/identity/IdentityService.java`
- `src/main/java/org/example/ggbot/service/subject/SubjectService.java`
- `src/main/java/org/example/ggbot/service/access/AccessControlService.java`
- `src/main/java/org/example/ggbot/service/conversation/ConversationService.java`
- `src/main/java/org/example/ggbot/service/memory/MemoryService.java`
- `src/main/java/org/example/ggbot/service/context/PersistentConversationContextService.java`
- `src/main/java/org/example/ggbot/service/binding/AccountBindingService.java`
- `src/main/java/org/example/ggbot/service/binding/RedisAccountBindingService.java`
- `src/main/java/org/example/ggbot/service/dto/ResolvedWebUser.java`
- `src/main/java/org/example/ggbot/service/dto/ResolvedFeishuUser.java`
- `src/main/java/org/example/ggbot/service/dto/ConversationContext.java`
- `src/main/java/org/example/ggbot/service/dto/FeishuInboundMessage.java`
- `src/main/java/org/example/ggbot/service/feishu/FeishuMessageHandler.java`
- `src/main/java/org/example/ggbot/adapter/web/dto/WebChatSendRequest.java`
- `src/main/java/org/example/ggbot/adapter/web/dto/WebChatSendResponse.java`
- `src/main/java/org/example/ggbot/adapter/web/dto/OrganizationResponse.java`
- `src/main/java/org/example/ggbot/adapter/web/dto/OrganizationSwitchRequest.java`
- `src/main/java/org/example/ggbot/adapter/web/dto/ConversationSummaryResponse.java`
- `src/main/java/org/example/ggbot/adapter/web/dto/MessageResponse.java`
- `src/main/java/org/example/ggbot/adapter/web/dto/MemoryResponse.java`
- `src/main/java/org/example/ggbot/adapter/web/dto/BindTokenResponse.java`
- `src/main/java/org/example/ggbot/adapter/web/WebChatController.java`
- `src/main/java/org/example/ggbot/adapter/web/OrganizationController.java`
- `src/main/java/org/example/ggbot/adapter/web/ConversationController.java`
- `src/main/java/org/example/ggbot/adapter/web/MemoryController.java`
- `src/main/java/org/example/ggbot/adapter/web/AccountBindingController.java`
- `src/test/java/org/example/ggbot/persistence/repository/OrganizationRepositoryTest.java`
- `src/test/java/org/example/ggbot/service/identity/IdentityServiceTest.java`
- `src/test/java/org/example/ggbot/service/access/AccessControlServiceTest.java`
- `src/test/java/org/example/ggbot/service/conversation/ConversationServiceTest.java`
- `src/test/java/org/example/ggbot/service/memory/MemoryServiceTest.java`
- `src/test/java/org/example/ggbot/service/feishu/FeishuMessageHandlerTest.java`
- `src/test/java/org/example/ggbot/adapter/web/WebChatControllerTest.java`
- `src/test/java/org/example/ggbot/adapter/web/OrganizationControllerTest.java`
- `src/test/java/org/example/ggbot/adapter/web/ConversationControllerTest.java`
- `src/test/java/org/example/ggbot/adapter/web/MemoryControllerTest.java`
- `src/test/java/org/example/ggbot/adapter/web/AccountBindingControllerTest.java`

### Modify

- `src/main/resources/application.yml`
- `src/main/java/org/example/ggbot/adapter/feishu/FeishuEventParser.java`
- `src/main/java/org/example/ggbot/adapter/feishu/FeishuWebhookController.java`
- `src/main/java/org/example/ggbot/agent/AgentRequest.java`
- `src/main/java/org/example/ggbot/agent/AgentService.java`
- `src/main/java/org/example/ggbot/common/ApiResponse.java`
- `src/test/java/org/example/ggbot/AgentPilotApplicationTests.java`

### Responsibility Notes

- `persistence/entity` contains the new multitenant source-of-truth data model.
- `persistence/repository` contains only `org_id`-aware query methods for conversations, messages, memory, and access checks.
- `service/*` contains all creation flows, access checks, and context assembly. Controllers should not build access SQL or infer tenant semantics.
- `adapter/web` contains only request parsing and response mapping for the new APIs.
- `adapter/feishu` stays responsible for webhook transport and event parsing; `FeishuMessageHandler` owns business semantics.

### Task 1: Add Schema, Enums, and Error Infrastructure

**Files:**
- Create: `src/main/resources/schema.sql`
- Create: `src/main/java/org/example/ggbot/enums/*.java`
- Create: `src/main/java/org/example/ggbot/exception/ForbiddenException.java`
- Create: `src/main/java/org/example/ggbot/exception/NotFoundException.java`
- Create: `src/main/java/org/example/ggbot/exception/BadRequestException.java`
- Create: `src/main/java/org/example/ggbot/exception/GlobalExceptionHandler.java`
- Modify: `src/main/resources/application.yml`
- Test: `src/test/java/org/example/ggbot/AgentPilotApplicationTests.java`
- Test: `src/test/java/org/example/ggbot/persistence/repository/OrganizationRepositoryTest.java`

- [ ] **Step 1: Write the failing repository and boot tests**

```java
package org.example.ggbot.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.ggbot.persistence.entity.OrganizationEntity;
import org.example.ggbot.enums.OrgStatus;
import org.example.ggbot.enums.OrganizationType;
import org.example.ggbot.enums.ProviderType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class OrganizationRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void shouldPersistOrganizationByProviderAndTenantKey() {
        OrganizationEntity entity = OrganizationEntity.builder()
                .id(1001L)
                .name("Tenant A")
                .provider(ProviderType.FEISHU)
                .orgType(OrganizationType.FEISHU_TENANT)
                .tenantKey("tenant-a")
                .status(OrgStatus.ACTIVE)
                .build();

        organizationRepository.save(entity);

        assertThat(organizationRepository.findByProviderAndTenantKey(ProviderType.FEISHU, "tenant-a"))
                .isPresent()
                .get()
                .extracting(OrganizationEntity::getName)
                .isEqualTo("Tenant A");
    }
}
```

```java
package org.example.ggbot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AgentPilotApplicationTests {

    @Test
    void contextLoadsWithSchemaSql() {
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -Dtest=OrganizationRepositoryTest,AgentPilotApplicationTests test`
Expected: FAIL with missing JPA entities, repository, or schema initialization errors.

- [ ] **Step 3: Add schema, enums, and exception classes**

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    nickname VARCHAR(100),
    avatar_url VARCHAR(500),
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE TABLE organizations (
    id BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    org_type VARCHAR(50) NOT NULL,
    tenant_key VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_organizations_provider_tenant UNIQUE (provider, tenant_key)
);

CREATE TABLE user_identities (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    org_id BIGINT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_id VARCHAR(200) NOT NULL,
    tenant_key VARCHAR(200) NOT NULL,
    extra_info TEXT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_user_identities_provider_tenant_provider_id UNIQUE (provider, tenant_key, provider_id)
);

CREATE TABLE user_orgs (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    org_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    joined_at DATETIME NOT NULL,
    left_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_user_orgs_user_org UNIQUE (user_id, org_id)
);

CREATE TABLE subjects (
    id BIGINT PRIMARY KEY,
    org_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    ref_id VARCHAR(200) NOT NULL,
    name VARCHAR(200) NULL,
    status VARCHAR(30) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_subjects_org_type_provider_ref UNIQUE (org_id, type, provider, ref_id)
);

CREATE TABLE group_members (
    id BIGINT PRIMARY KEY,
    org_id BIGINT NOT NULL,
    group_subject_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    joined_at DATETIME NOT NULL,
    left_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_group_members_group_user UNIQUE (group_subject_id, user_id)
);

CREATE TABLE conversations (
    id BIGINT PRIMARY KEY,
    org_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    title VARCHAR(300) NULL,
    source VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_by_user_id BIGINT NULL,
    last_message_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE INDEX idx_conversations_org_subject_last ON conversations (org_id, subject_id, last_message_at);
CREATE INDEX idx_conversations_org_last ON conversations (org_id, last_message_at);

CREATE TABLE messages (
    id BIGINT PRIMARY KEY,
    org_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    sender_user_id BIGINT NULL,
    role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    provider_message_id VARCHAR(200) NULL,
    created_at DATETIME NOT NULL
);

CREATE INDEX idx_messages_org_conversation_created ON messages (org_id, conversation_id, created_at);
CREATE UNIQUE INDEX uk_messages_provider_message_id_org
    ON messages (provider_message_id, org_id);

CREATE TABLE memory (
    id BIGINT PRIMARY KEY,
    org_id BIGINT NOT NULL,
    subject_id BIGINT NOT NULL,
    memory_type VARCHAR(50) NOT NULL,
    scope VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    embedding_id VARCHAR(200) NULL,
    source_conversation_id BIGINT NULL,
    created_by_user_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE INDEX idx_memory_org_subject ON memory (org_id, subject_id);
CREATE INDEX idx_memory_org_type_scope ON memory (org_id, memory_type, scope);
```

```java
public enum ProviderType {
    WEB,
    FEISHU,
    SYSTEM
}
```

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage()));
    }
}
```

```yaml
spring:
  sql:
    init:
      mode: always
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -Dtest=OrganizationRepositoryTest,AgentPilotApplicationTests test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/schema.sql src/main/resources/application.yml src/main/java/org/example/ggbot/enums src/main/java/org/example/ggbot/exception src/test/java/org/example/ggbot/persistence/repository/OrganizationRepositoryTest.java src/test/java/org/example/ggbot/AgentPilotApplicationTests.java
git commit -m "feat: add multitenant schema and error infrastructure"
```

### Task 2: Add JPA Entities and Repositories

**Files:**
- Create: `src/main/java/org/example/ggbot/persistence/entity/*.java`
- Create: `src/main/java/org/example/ggbot/persistence/repository/*.java`
- Test: `src/test/java/org/example/ggbot/persistence/repository/OrganizationRepositoryTest.java`

- [ ] **Step 1: Extend the repository test with subject and message access patterns**

```java
@Test
void shouldFindUserSubjectByOrgAndRefId() {
    SubjectEntity subject = SubjectEntity.builder()
            .id(2001L)
            .orgId(1001L)
            .type(SubjectType.USER)
            .provider(ProviderType.SYSTEM)
            .refId("3001")
            .status("active")
            .build();

    subjectRepository.save(subject);

    assertThat(subjectRepository.findByOrgIdAndTypeAndProviderAndRefId(
            1001L, SubjectType.USER, ProviderType.SYSTEM, "3001")).isPresent();
}

@Test
void shouldListMessagesByOrgAndConversationOrderedByCreatedAt() {
    List<MessageEntity> messages = messageRepository.findByOrgIdAndConversationIdOrderByCreatedAtAsc(1001L, 4001L);
    assertThat(messages).hasSize(2);
}
```

- [ ] **Step 2: Run the repository test to verify it fails**

Run: `mvn -Dtest=OrganizationRepositoryTest test`
Expected: FAIL with missing entity mappings or repository beans.

- [ ] **Step 3: Add entities and repository methods**

```java
@Entity
@Table(name = "subjects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subjects_org_type_provider_ref", columnNames = {
                "org_id", "type", "provider", "ref_id"
        })
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SubjectEntity {

    @Id
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private SubjectType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private ProviderType provider;

    @Column(name = "ref_id", nullable = false, length = 200)
    private String refId;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

```java
public interface SubjectRepository extends JpaRepository<SubjectEntity, Long> {

    Optional<SubjectEntity> findByOrgIdAndTypeAndProviderAndRefId(
            Long orgId, SubjectType type, ProviderType provider, String refId);

    Optional<SubjectEntity> findByIdAndOrgId(Long id, Long orgId);
}
```

```java
public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    List<MessageEntity> findByOrgIdAndConversationIdOrderByCreatedAtAsc(Long orgId, Long conversationId);
}
```

- [ ] **Step 4: Run the repository test to verify it passes**

Run: `mvn -Dtest=OrganizationRepositoryTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/ggbot/persistence/entity src/main/java/org/example/ggbot/persistence/repository src/test/java/org/example/ggbot/persistence/repository/OrganizationRepositoryTest.java
git commit -m "feat: add multitenant persistence entities and repositories"
```

### Task 3: Implement Organization and Identity Bootstrapping

**Files:**
- Create: `src/main/java/org/example/ggbot/tenant/TenantContext.java`
- Create: `src/main/java/org/example/ggbot/service/dto/ResolvedWebUser.java`
- Create: `src/main/java/org/example/ggbot/service/dto/ResolvedFeishuUser.java`
- Create: `src/main/java/org/example/ggbot/service/organization/OrganizationService.java`
- Create: `src/main/java/org/example/ggbot/service/identity/IdentityService.java`
- Test: `src/test/java/org/example/ggbot/service/identity/IdentityServiceTest.java`

- [ ] **Step 1: Write failing identity service tests**

```java
@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserIdentityRepository userIdentityRepository;
    @Mock private UserOrgRepository userOrgRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private IdGenerator idGenerator;

    @InjectMocks private OrganizationService organizationService;

    @Test
    void shouldCreatePersonalOrganizationForWebIdentity() {
        IdentityService service = new IdentityService(
                userRepository, userIdentityRepository, userOrgRepository, subjectRepository,
                organizationService, idGenerator);

        when(userIdentityRepository.findByProviderAndTenantKeyAndProviderId(
                ProviderType.WEB, "GLOBAL", "web-user-key-1")).thenReturn(Optional.empty());

        ResolvedWebUser resolved = service.getOrCreateUserByWebSession("web-user-key-1");

        assertThat(resolved.personalOrg().getProvider()).isEqualTo(ProviderType.WEB);
        assertThat(resolved.personalOrg().getTenantKey()).isEqualTo("personal:" + resolved.user().getId());
    }

    @Test
    void shouldScopeFeishuIdentityByTenantKey() {
        IdentityService service = new IdentityService(
                userRepository, userIdentityRepository, userOrgRepository, subjectRepository,
                organizationService, idGenerator);

        when(userIdentityRepository.findByProviderAndTenantKeyAndProviderId(
                ProviderType.FEISHU, "tenant-a", "ou_xxx")).thenReturn(Optional.empty());

        ResolvedFeishuUser resolved = service.getOrCreateUserByFeishuOpenId(
                "ou_xxx", "tenant-a", "Tenant A", "Alice", "https://img");

        assertThat(resolved.org().getTenantKey()).isEqualTo("tenant-a");
    }
}
```

- [ ] **Step 2: Run the identity test to verify it fails**

Run: `mvn -Dtest=IdentityServiceTest test`
Expected: FAIL with missing service classes or unresolved methods.

- [ ] **Step 3: Implement organization and identity services**

```java
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserOrgRepository userOrgRepository;
    private final IdGenerator idGenerator;

    @Transactional
    public OrganizationEntity getOrCreateFeishuOrganization(String tenantKey, String tenantName) {
        return organizationRepository.findByProviderAndTenantKey(ProviderType.FEISHU, tenantKey)
                .orElseGet(() -> organizationRepository.save(OrganizationEntity.builder()
                        .id(idGenerator.nextId())
                        .name(tenantName == null || tenantName.isBlank() ? tenantKey : tenantName)
                        .provider(ProviderType.FEISHU)
                        .orgType(OrganizationType.FEISHU_TENANT)
                        .tenantKey(tenantKey)
                        .status(OrgStatus.ACTIVE)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    public void checkUserActiveInOrg(Long userId, Long orgId) {
        boolean active = userOrgRepository.existsByUserIdAndOrgIdAndStatus(userId, orgId, UserOrgStatus.ACTIVE);
        if (!active) {
            throw new ForbiddenException("User is not active in organization");
        }
    }
}
```

```java
@Service
@RequiredArgsConstructor
public class IdentityService {

    private static final String WEB_GLOBAL_TENANT = "GLOBAL";

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserOrgRepository userOrgRepository;
    private final SubjectRepository subjectRepository;
    private final OrganizationService organizationService;
    private final IdGenerator idGenerator;

    @Transactional
    public ResolvedWebUser getOrCreateUserByWebSession(String webUserKey) {
        return userIdentityRepository.findByProviderAndTenantKeyAndProviderId(
                        ProviderType.WEB, WEB_GLOBAL_TENANT, webUserKey)
                .map(identity -> ensureWebGraph(identity.getUserId()))
                .orElseGet(() -> createWebGraph(webUserKey));
    }
}
```

- [ ] **Step 4: Run the identity test to verify it passes**

Run: `mvn -Dtest=IdentityServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/ggbot/tenant src/main/java/org/example/ggbot/service/dto src/main/java/org/example/ggbot/service/organization src/main/java/org/example/ggbot/service/identity src/test/java/org/example/ggbot/service/identity/IdentityServiceTest.java
git commit -m "feat: add organization and identity bootstrap services"
```

### Task 4: Implement Subject, Access Control, Conversations, and Memory

**Files:**
- Create: `src/main/java/org/example/ggbot/service/subject/SubjectService.java`
- Create: `src/main/java/org/example/ggbot/service/access/AccessControlService.java`
- Create: `src/main/java/org/example/ggbot/service/conversation/ConversationService.java`
- Create: `src/main/java/org/example/ggbot/service/memory/MemoryService.java`
- Test: `src/test/java/org/example/ggbot/service/access/AccessControlServiceTest.java`
- Test: `src/test/java/org/example/ggbot/service/conversation/ConversationServiceTest.java`
- Test: `src/test/java/org/example/ggbot/service/memory/MemoryServiceTest.java`

- [ ] **Step 1: Write failing service tests**

```java
@Test
void shouldAllowAccessToOwnUserSubjectOnlyInsideCurrentOrg() {
    when(userOrgRepository.existsByUserIdAndOrgIdAndStatus(3001L, 1001L, UserOrgStatus.ACTIVE)).thenReturn(true);
    when(subjectRepository.findByIdAndOrgId(5001L, 1001L)).thenReturn(Optional.of(
            SubjectEntity.builder()
                    .id(5001L)
                    .orgId(1001L)
                    .type(SubjectType.USER)
                    .provider(ProviderType.SYSTEM)
                    .refId("3001")
                    .build()));

    assertThat(accessControlService.canAccessSubject(3001L, 1001L, 5001L)).isTrue();
}

@Test
void shouldListAccessibleConversationsForOwnSubjectAndActiveGroups() {
    when(conversationRepository.findAccessibleConversations(3001L, "3001", 1001L))
            .thenReturn(List.of(ConversationEntity.builder().id(7001L).orgId(1001L).build()));

    List<ConversationEntity> conversations = conversationService.listAccessibleConversations(3001L, 1001L);

    assertThat(conversations).extracting(ConversationEntity::getId).containsExactly(7001L);
}

@Test
void shouldListOnlyMemoryVisibleThroughAccessibleSubjects() {
    when(memoryRepository.findAccessibleMemory(3001L, "3001", 1001L))
            .thenReturn(List.of(MemoryEntity.builder().id(8001L).orgId(1001L).content("shared").build()));

    assertThat(memoryService.listAccessibleMemory(3001L, 1001L)).hasSize(1);
}
```

- [ ] **Step 2: Run service tests to verify they fail**

Run: `mvn -Dtest=AccessControlServiceTest,ConversationServiceTest,MemoryServiceTest test`
Expected: FAIL with missing services or custom repository queries.

- [ ] **Step 3: Implement subject, access, conversation, and memory services**

```java
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserOrgRepository userOrgRepository;
    private final SubjectRepository subjectRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ConversationRepository conversationRepository;

    public boolean canAccessSubject(Long userId, Long orgId, Long subjectId) {
        if (!userOrgRepository.existsByUserIdAndOrgIdAndStatus(userId, orgId, UserOrgStatus.ACTIVE)) {
            return false;
        }
        SubjectEntity subject = subjectRepository.findByIdAndOrgId(subjectId, orgId)
                .orElseThrow(() -> new NotFoundException("Subject not found"));
        if (subject.getType() == SubjectType.USER) {
            return subject.getProvider() == ProviderType.SYSTEM
                    && String.valueOf(userId).equals(subject.getRefId());
        }
        return groupMemberRepository.existsByOrgIdAndGroupSubjectIdAndUserIdAndStatus(
                orgId, subjectId, userId, MemberStatus.ACTIVE);
    }
}
```

```java
public interface ConversationRepository extends JpaRepository<ConversationEntity, Long> {

    @Query("""
            select c from ConversationEntity c
            where c.orgId = :orgId
              and c.subjectId in (
                select s.id from SubjectEntity s
                 where s.orgId = :orgId
                   and s.type = org.example.ggbot.enums.SubjectType.USER
                   and s.provider = org.example.ggbot.enums.ProviderType.SYSTEM
                   and s.refId = :userIdRef
                union
                select gm.groupSubjectId from GroupMemberEntity gm
                 where gm.orgId = :orgId
                   and gm.userId = :userId
                   and gm.status = org.example.ggbot.enums.MemberStatus.ACTIVE
              )
            order by c.lastMessageAt desc
            """)
    List<ConversationEntity> findAccessibleConversations(
            @Param("userId") Long userId,
            @Param("userIdRef") String userIdRef,
            @Param("orgId") Long orgId);
}
```

```java
public interface MemoryRepository extends JpaRepository<MemoryEntity, Long> {

    @Query("""
            select m from MemoryEntity m
            where m.orgId = :orgId
              and m.subjectId in (
                select s.id from SubjectEntity s
                 where s.orgId = :orgId
                   and s.type = org.example.ggbot.enums.SubjectType.USER
                   and s.provider = org.example.ggbot.enums.ProviderType.SYSTEM
                   and s.refId = :userIdRef
                union
                select gm.groupSubjectId from GroupMemberEntity gm
                 where gm.orgId = :orgId
                   and gm.userId = :userId
                   and gm.status = org.example.ggbot.enums.MemberStatus.ACTIVE
              )
            order by m.updatedAt desc
            """)
    List<MemoryEntity> findAccessibleMemory(
            @Param("userId") Long userId,
            @Param("userIdRef") String userIdRef,
            @Param("orgId") Long orgId);
}
```

- [ ] **Step 4: Run service tests to verify they pass**

Run: `mvn -Dtest=AccessControlServiceTest,ConversationServiceTest,MemoryServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/ggbot/service/subject src/main/java/org/example/ggbot/service/access src/main/java/org/example/ggbot/service/conversation src/main/java/org/example/ggbot/service/memory src/main/java/org/example/ggbot/persistence/repository src/test/java/org/example/ggbot/service/access/AccessControlServiceTest.java src/test/java/org/example/ggbot/service/conversation/ConversationServiceTest.java src/test/java/org/example/ggbot/service/memory/MemoryServiceTest.java
git commit -m "feat: add multitenant access, conversation, and memory services"
```

### Task 5: Add Persistent Agent Context Assembly

**Files:**
- Create: `src/main/java/org/example/ggbot/service/dto/ConversationContext.java`
- Create: `src/main/java/org/example/ggbot/service/context/PersistentConversationContextService.java`
- Modify: `src/main/java/org/example/ggbot/agent/AgentRequest.java`
- Modify: `src/main/java/org/example/ggbot/agent/AgentService.java`
- Test: `src/test/java/org/example/ggbot/service/conversation/ConversationServiceTest.java`

- [ ] **Step 1: Write a failing context assembly test**

```java
@Test
void shouldBuildAgentContextFromConversationMessagesAndGlobalSubjectMemory() {
    when(messageRepository.findTop20ByOrgIdAndConversationIdOrderByCreatedAtDesc(1001L, 7001L))
            .thenReturn(List.of(
                    MessageEntity.builder().role(MessageRole.USER).content("hello").build(),
                    MessageEntity.builder().role(MessageRole.ASSISTANT).content("hi").build()));
    when(memoryRepository.findByOrgIdAndSubjectIdAndScopeOrderByUpdatedAtDesc(
            1001L, 5001L, MemoryScope.GLOBAL)).thenReturn(List.of(
                    MemoryEntity.builder().content("user likes concise answers").build()));

    ConversationContext context = persistentConversationContextService.buildContext(1001L, 5001L, 7001L);

    assertThat(context.history()).containsExactly("USER: hello", "ASSISTANT: hi");
    assertThat(context.globalMemory()).containsExactly("user likes concise answers");
}
```

- [ ] **Step 2: Run the context test to verify it fails**

Run: `mvn -Dtest=ConversationServiceTest test`
Expected: FAIL with missing context assembler or query methods.

- [ ] **Step 3: Implement the context assembler and Agent request changes**

```java
public record ConversationContext(
        Long orgId,
        Long subjectId,
        Long conversationId,
        List<String> history,
        List<String> globalMemory
) {
}
```

```java
@Service
@RequiredArgsConstructor
public class PersistentConversationContextService {

    private final MessageRepository messageRepository;
    private final MemoryRepository memoryRepository;

    public ConversationContext buildContext(Long orgId, Long subjectId, Long conversationId) {
        List<String> history = messageRepository
                .findTop20ByOrgIdAndConversationIdOrderByCreatedAtDesc(orgId, conversationId)
                .stream()
                .sorted(Comparator.comparing(MessageEntity::getCreatedAt))
                .map(message -> message.getRole().name() + ": " + message.getContent())
                .toList();

        List<String> globalMemory = memoryRepository
                .findByOrgIdAndSubjectIdAndScopeOrderByUpdatedAtDesc(orgId, subjectId, MemoryScope.GLOBAL)
                .stream()
                .map(MemoryEntity::getContent)
                .toList();

        return new ConversationContext(orgId, subjectId, conversationId, history, globalMemory);
    }
}
```

```java
public record AgentRequest(
        String sessionId,
        String userId,
        String message,
        AgentChannel channel,
        String externalConversationId,
        String conversationId,
        Map<String, Object> attributes,
        List<String> history,
        List<String> memory
) {
}
```

- [ ] **Step 4: Run the context test to verify it passes**

Run: `mvn -Dtest=ConversationServiceTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/ggbot/service/context src/main/java/org/example/ggbot/service/dto/ConversationContext.java src/main/java/org/example/ggbot/agent/AgentRequest.java src/main/java/org/example/ggbot/agent/AgentService.java src/test/java/org/example/ggbot/service/conversation/ConversationServiceTest.java
git commit -m "feat: assemble agent context from persistent conversation data"
```

### Task 6: Implement Web Workspace, Conversation, Memory, and Binding APIs

**Files:**
- Create: `src/main/java/org/example/ggbot/adapter/web/dto/*.java`
- Create: `src/main/java/org/example/ggbot/adapter/web/WebChatController.java`
- Create: `src/main/java/org/example/ggbot/adapter/web/OrganizationController.java`
- Create: `src/main/java/org/example/ggbot/adapter/web/ConversationController.java`
- Create: `src/main/java/org/example/ggbot/adapter/web/MemoryController.java`
- Create: `src/main/java/org/example/ggbot/adapter/web/AccountBindingController.java`
- Create: `src/main/java/org/example/ggbot/service/binding/AccountBindingService.java`
- Create: `src/main/java/org/example/ggbot/service/binding/RedisAccountBindingService.java`
- Test: `src/test/java/org/example/ggbot/adapter/web/WebChatControllerTest.java`
- Test: `src/test/java/org/example/ggbot/adapter/web/OrganizationControllerTest.java`
- Test: `src/test/java/org/example/ggbot/adapter/web/ConversationControllerTest.java`
- Test: `src/test/java/org/example/ggbot/adapter/web/MemoryControllerTest.java`
- Test: `src/test/java/org/example/ggbot/adapter/web/AccountBindingControllerTest.java`

- [ ] **Step 1: Write failing controller tests**

```java
@Test
void shouldSendWebMessageInPersonalWorkspaceWhenOrgIdMissing() throws Exception {
    when(identityService.getOrCreateUserByWebSession("web-user-key-1"))
            .thenReturn(new ResolvedWebUser(userEntity, personalOrg));
    when(conversationService.createConversation(1001L, 5001L, "web", "hello", 3001L))
            .thenReturn(conversationEntity);

    mockMvc.perform(post("/api/web/chat/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"sessionId":"legacy-session","messageContent":"hello"}
                            """)
                    .cookie(new Cookie("web_user_key", "web-user-key-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orgId").value(1001L));
}

@Test
void shouldReturnAccessibleOrganizations() throws Exception {
    mockMvc.perform(get("/api/orgs").cookie(new Cookie("web_user_key", "web-user-key-1")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].tenantKey").value("personal:3001"));
}

@Test
void shouldRejectConversationMessageQueryWithoutAccess() throws Exception {
    when(accessControlService.checkCanAccessConversation(3001L, 1001L, 7001L))
            .thenThrow(new ForbiddenException("forbidden"));

    mockMvc.perform(get("/api/conversations/7001/messages").param("orgId", "1001"))
            .andExpect(status().isForbidden());
}
```

- [ ] **Step 2: Run controller tests to verify they fail**

Run: `mvn -Dtest=WebChatControllerTest,OrganizationControllerTest,ConversationControllerTest,MemoryControllerTest,AccountBindingControllerTest test`
Expected: FAIL with missing DTOs, controllers, or binding service.

- [ ] **Step 3: Implement Web controllers and DTOs**

```java
@RestController
@RequestMapping("/api/web/chat")
@RequiredArgsConstructor
public class WebChatController {

    private final IdentityService identityService;
    private final OrganizationService organizationService;
    private final SubjectService subjectService;
    private final ConversationService conversationService;
    private final PersistentConversationContextService contextService;
    private final AgentService agentService;

    @PostMapping("/send")
    public ApiResponse<WebChatSendResponse> send(
            @CookieValue("web_user_key") String webUserKey,
            @RequestBody @Valid WebChatSendRequest request) {
        ResolvedWebUser resolved = identityService.getOrCreateUserByWebSession(webUserKey);
        Long currentOrgId = request.orgId() == null ? resolved.personalOrg().getId() : request.orgId();
        organizationService.checkUserActiveInOrg(resolved.user().getId(), currentOrgId);
        SubjectEntity subject = subjectService.getOrCreateUserSubject(resolved.user().getId(), currentOrgId);
        ConversationEntity conversation = conversationService.createOrReuseWebConversation(
                currentOrgId, subject.getId(), request.conversationId(), request.messageContent(), resolved.user().getId());
        conversationService.addMessage(currentOrgId, conversation.getId(), resolved.user().getId(),
                MessageRole.USER, request.messageContent(), "text", request.sessionId());
        ConversationContext context = contextService.buildContext(currentOrgId, subject.getId(), conversation.getId());
        String reply = agentService.replyWithPersistentContext(context, request.messageContent());
        conversationService.addMessage(currentOrgId, conversation.getId(), null,
                MessageRole.ASSISTANT, reply, "text", null);
        return ApiResponse.success(new WebChatSendResponse(currentOrgId, conversation.getId(), reply));
    }
}
```

```java
public interface AccountBindingService {

    String createBindToken(Long userId, Long currentOrgId);

    void bindFeishuIdentity(String token, String openId, String tenantKey, String tenantName);

    void mergeUsers(Long sourceUserId, Long targetUserId);
}
```

```java
@Service
public class RedisAccountBindingService implements AccountBindingService {

    @Override
    public void mergeUsers(Long sourceUserId, Long targetUserId) {
        throw new UnsupportedOperationException("""
                TODO: merge user_identities, user_orgs, group_members, messages.sender_user_id,
                and reconcile per-org user subjects before enabling user merge
                """);
    }
}
```

- [ ] **Step 4: Run controller tests to verify they pass**

Run: `mvn -Dtest=WebChatControllerTest,OrganizationControllerTest,ConversationControllerTest,MemoryControllerTest,AccountBindingControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/ggbot/adapter/web src/main/java/org/example/ggbot/service/binding src/test/java/org/example/ggbot/adapter/web/WebChatControllerTest.java src/test/java/org/example/ggbot/adapter/web/OrganizationControllerTest.java src/test/java/org/example/ggbot/adapter/web/ConversationControllerTest.java src/test/java/org/example/ggbot/adapter/web/MemoryControllerTest.java src/test/java/org/example/ggbot/adapter/web/AccountBindingControllerTest.java
git commit -m "feat: add multitenant web workspace and history APIs"
```

### Task 7: Implement Feishu Multitenant Message Handling

**Files:**
- Create: `src/main/java/org/example/ggbot/service/dto/FeishuInboundMessage.java`
- Create: `src/main/java/org/example/ggbot/service/feishu/FeishuMessageHandler.java`
- Modify: `src/main/java/org/example/ggbot/adapter/feishu/FeishuEventParser.java`
- Modify: `src/main/java/org/example/ggbot/adapter/feishu/FeishuWebhookController.java`
- Test: `src/test/java/org/example/ggbot/service/feishu/FeishuMessageHandlerTest.java`
- Test: `src/test/java/org/example/ggbot/adapter/feishu/FeishuWebhookControllerTest.java`

- [ ] **Step 1: Write failing Feishu handler tests**

```java
@Test
void shouldRouteGroupMessageToGroupSubjectWithinTenantOrganization() {
    FeishuInboundMessage inbound = new FeishuInboundMessage(
            "tenant-a", "Tenant A", "ou_xxx", "oc_group", "Architecture Group",
            "group", "msg_1", "hello", "Alice", "https://img");

    handler.handle(inbound);

    verify(subjectService).getOrCreateFeishuGroupSubject("oc_group", "Architecture Group", 1001L);
    verify(subjectService).ensureGroupMember(1001L, 5002L, 3001L);
    verify(conversationService).addMessage(1001L, 7001L, 3001L, MessageRole.USER, "hello", "text", "msg_1");
}

@Test
void shouldIgnoreUnsupportedWebhookEventTypes() throws Exception {
    mockMvc.perform(post("/feishu/webhook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"header\":{\"event_type\":\"contact.user.deleted_v3\"}}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("ignored"));
}
```

- [ ] **Step 2: Run Feishu tests to verify they fail**

Run: `mvn -Dtest=FeishuMessageHandlerTest,FeishuWebhookControllerTest test`
Expected: FAIL with missing handler or parser mapping.

- [ ] **Step 3: Implement the Feishu business handler and controller integration**

```java
@Service
@RequiredArgsConstructor
public class FeishuMessageHandler {

    private final IdentityService identityService;
    private final SubjectService subjectService;
    private final ConversationService conversationService;
    private final PersistentConversationContextService contextService;
    private final AgentService agentService;

    @Transactional
    public void handle(FeishuInboundMessage inbound) {
        ResolvedFeishuUser resolved = identityService.getOrCreateUserByFeishuOpenId(
                inbound.openId(), inbound.tenantKey(), inbound.tenantName(), inbound.senderNickname(), inbound.senderAvatar());
        Long orgId = resolved.org().getId();
        SubjectEntity subject = "group".equalsIgnoreCase(inbound.chatType())
                ? subjectService.getOrCreateFeishuGroupSubject(inbound.chatId(), inbound.chatName(), orgId)
                : subjectService.getOrCreateUserSubject(resolved.user().getId(), orgId);
        if (subject.getType() == SubjectType.GROUP) {
            subjectService.ensureGroupMember(orgId, subject.getId(), resolved.user().getId());
        }
        ConversationEntity conversation = conversationService.getOrCreateActiveConversation(
                orgId, subject.getId(), "feishu", resolved.user().getId());
        conversationService.addMessage(orgId, conversation.getId(), resolved.user().getId(),
                MessageRole.USER, inbound.messageContent(), "text", inbound.messageId());
        ConversationContext context = contextService.buildContext(orgId, subject.getId(), conversation.getId());
        String reply = agentService.replyWithPersistentContext(context, inbound.messageContent());
        conversationService.addMessage(orgId, conversation.getId(), null,
                MessageRole.ASSISTANT, reply, "text", null);
    }
}
```

```java
AgentRequest agentRequest = feishuEventParser.toAgentRequest(request, context);
```

- [ ] **Step 4: Run Feishu tests to verify they pass**

Run: `mvn -Dtest=FeishuMessageHandlerTest,FeishuWebhookControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/example/ggbot/service/feishu src/main/java/org/example/ggbot/service/dto/FeishuInboundMessage.java src/main/java/org/example/ggbot/adapter/feishu/FeishuEventParser.java src/main/java/org/example/ggbot/adapter/feishu/FeishuWebhookController.java src/test/java/org/example/ggbot/service/feishu/FeishuMessageHandlerTest.java src/test/java/org/example/ggbot/adapter/feishu/FeishuWebhookControllerTest.java
git commit -m "feat: add multitenant feishu message handling"
```

### Task 8: Run End-to-End Regression and Finalize Docs

**Files:**
- Modify: `docs/superpowers/specs/2026-05-01-multitenant-user-system-design.md`
- Modify: `README.md`

- [ ] **Step 1: Write a short checklist note back into the spec**

```markdown
## Implementation Notes

- Web identity uses stable `web_user_key` cookie instead of raw session ID.
- Database-backed messages and memory are the only supported source for new multitenant chat flows.
- Old `/api/chat/send` remains transitional and is not the source of truth for new workspace APIs.
```

- [ ] **Step 2: Run focused regression suites**

Run: `mvn -Dtest=OrganizationRepositoryTest,IdentityServiceTest,AccessControlServiceTest,ConversationServiceTest,MemoryServiceTest,WebChatControllerTest,OrganizationControllerTest,ConversationControllerTest,MemoryControllerTest,AccountBindingControllerTest,FeishuMessageHandlerTest,FeishuWebhookControllerTest test`
Expected: PASS

Run: `mvn test`
Expected: PASS

- [ ] **Step 3: Update README integration notes**

```markdown
## Multitenant Collaboration

- Web users receive a personal workspace automatically.
- Feishu `tenant_key` is mapped to an internal organization.
- Conversation, message, and memory queries are always scoped by `org_id`.
- Group chat history and memory are shared only with active group members in the same organization.
```

- [ ] **Step 4: Re-run smoke tests**

Run: `mvn -Dtest=AgentPilotApplicationTests test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add docs/superpowers/specs/2026-05-01-multitenant-user-system-design.md README.md
git commit -m "docs: document multitenant collaboration behavior"
```

## Self-Review

### Spec Coverage

- DDL and enum requirements are covered by Task 1.
- Entity, repository, and `org_id` scoped query requirements are covered by Task 2 and Task 4.
- `OrganizationService`, `IdentityService`, `SubjectService`, `AccessControlService`, `ConversationService`, and `MemoryService` are covered by Tasks 3 to 5.
- `FeishuMessageHandler` requirements are covered by Task 7.
- `WebChatController`, organization APIs, conversation APIs, memory APIs, and bind token API are covered by Task 6.
- Account binding skeleton and explicit merge TODO scope are covered by Task 6.
- Database-backed Agent context is covered by Task 5.
- Regression and documentation handoff are covered by Task 8.

### Placeholder Scan

- No `TODO`, `TBD`, or “implement later” placeholders remain in executable tasks.
- The only intentional `TODO` is inside `mergeUsers`, because the approved spec explicitly requires a non-complete skeleton with documented migration scope.

### Type Consistency

- `ProviderType`, `SubjectType`, `MessageRole`, `MemoryScope`, and `UserOrgStatus` are used consistently across repository, service, and test snippets.
- New Web response and request naming stays under `adapter/web/dto` and does not reuse legacy session DTO names.

