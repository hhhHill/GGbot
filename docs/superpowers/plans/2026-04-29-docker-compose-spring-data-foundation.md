# Docker Compose and Spring Data Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a local MySQL and Redis compose stack plus the Spring dependencies and profile config needed for later database table and service work.

**Architecture:** Keep the default app start path working with a lightweight embedded H2 fallback so existing tests continue to run. Add a dedicated `docker` profile for future containerized runs against MySQL and Redis, and place those services in a single compose network with health checks and localhost-only port bindings for local development.

**Tech Stack:** Docker Compose, MySQL 8.3, Redis 7.2, Spring Boot 3.5, Spring Data JPA, Spring Data Redis, MySQL Connector/J.

---

### Task 1: Add the local MySQL and Redis compose stack

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: Define MySQL and Redis services with health checks and localhost-only bindings**

```yaml
version: "3.8"

services:
  mysql:
    image: mysql:8.3
    container_name: ggbot-mysql
    restart: always
    ports:
      - "127.0.0.1:3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-123456}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-ggbot}
      MYSQL_USER: ${MYSQL_USER:-ggbot}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-ggbot123}
      TZ: Asia/Shanghai
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --max_connections=1000
    volumes:
      - ./mysql/data:/var/lib/mysql
      - ./mysql/conf:/etc/mysql/conf.d
      - ./mysql/init:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "mysqladmin ping -h 127.0.0.1 -uroot -p$$MYSQL_ROOT_PASSWORD"]
      interval: 5s
      timeout: 3s
      retries: 10

  redis:
    image: redis:7.2
    container_name: ggbot-redis
    restart: always
    ports:
      - "127.0.0.1:6379:6379"
    command: >
      redis-server
      --requirepass ${REDIS_PASSWORD:-123456}
      --appendonly yes
    volumes:
      - ./redis/data:/data
    healthcheck:
      test: ["CMD-SHELL", "redis-cli -a $$REDIS_PASSWORD ping"]
      interval: 3s
      timeout: 2s
      retries: 5
    environment:
      REDIS_PASSWORD: ${REDIS_PASSWORD:-123456}

networks:
  default:
    name: ggbot-network
```

- [ ] **Step 2: Verify the file parses as valid compose YAML**

Run: `docker compose config`
Expected: output renders the merged compose config without YAML or interpolation errors.

### Task 2: Add Spring dependencies and docker-profile config

**Files:**
- Modify: `pom.xml`
- Create: `src/main/resources/application-docker.yml`

- [ ] **Step 1: Add Spring Data JPA, Spring Data Redis, and MySQL Connector/J**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

- [ ] **Step 2: Add a dedicated docker profile for future containerized app runs**

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:mysql://mysql:3306/ggbot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true}
    username: ${SPRING_DATASOURCE_USERNAME:ggbot}
    password: ${SPRING_DATASOURCE_PASSWORD:ggbot123}
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: ${SPRING_DATA_REDIS_HOST:redis}
      port: ${SPRING_DATA_REDIS_PORT:6379}
      password: ${SPRING_DATA_REDIS_PASSWORD:123456}
      timeout: 3s
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
```

- [ ] **Step 3: Confirm the default profile still starts without external services**

Run: `mvn -q test`
Expected: existing tests still run under the default profile without requiring MySQL or Redis, while the `docker` profile can point at the compose stack later.
