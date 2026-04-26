# 混合同步/异步 LLM 执行实施计划

> **供代理执行者使用：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐步落实本计划。步骤使用复选框语法（`- [ ]`）追踪进度。

**目标：** 增加统一可靠的 LLM 调用层和混合同步/异步执行能力，让短请求继续同步返回，让长时间运行的模型/工具请求返回 `jobId`、暴露进度、支持 watchdog 超时处理，并允许用户重试。

**架构：** 保持现有 Agent 内核作为唯一执行主路径。先增加带重试、超时和兜底的可靠模型调用层，再把长请求包裹进任务系统，包括判定器、worker、watchdog 和轮询接口。前端保持简单：每个会话同一时间只处理一个活动请求，同步请求展示加载态，异步请求轮询状态，失败后可重试。

**技术栈：** Java 17、Spring Boot 3.4、Spring AI、首版使用内存任务存储、浏览器原生 JS 前端、JUnit 5、Mockito、Maven。

---

## 文件映射

**新增：**
- `src/main/java/org/example/ggbot/ai/ChatFallbackPolicy.java`
- `src/main/java/org/example/ggbot/ai/ReliableChatService.java`
- `src/main/java/org/example/ggbot/job/JobStatus.java`
- `src/main/java/org/example/ggbot/job/JobRecord.java`
- `src/main/java/org/example/ggbot/job/JobService.java`
- `src/main/java/org/example/ggbot/job/InMemoryJobService.java`
- `src/main/java/org/example/ggbot/job/AsyncExecutionMode.java`
- `src/main/java/org/example/ggbot/job/AsyncExecutionDecider.java`
- `src/main/java/org/example/ggbot/job/JobWorker.java`
- `src/main/java/org/example/ggbot/job/JobWatchdog.java`
- `src/main/java/org/example/ggbot/adapter/web/dto/WebChatAcceptedResponse.java`
- `src/main/java/org/example/ggbot/adapter/web/dto/WebJobStatusResponse.java`
- `src/test/java/org/example/ggbot/ai/ReliableChatServiceTest.java`
- `src/test/java/org/example/ggbot/job/InMemoryJobServiceTest.java`
- `src/test/java/org/example/ggbot/job/AsyncExecutionDeciderTest.java`
- `src/test/java/org/example/ggbot/job/JobWorkerTest.java`
- `src/test/java/org/example/ggbot/job/JobWatchdogTest.java`
- `src/test/java/org/example/ggbot/adapter/web/WebJobControllerTest.java`

**修改：**
- `src/main/java/org/example/ggbot/ai/SpringAiChatService.java`
- `src/main/java/org/example/ggbot/tool/impl/SummarizeTool.java`
- `src/main/java/org/example/ggbot/adapter/web/WebAgentController.java`
- `src/main/java/org/example/ggbot/adapter/web/dto/WebChatResponse.java`
- `src/main/java/org/example/ggbot/config/AppConfig.java`
- `src/main/resources/static/app.js`
- `src/main/resources/static/index.html`
- `src/main/resources/static/app.css`
- `src/main/resources/application.yml`

### 任务 1：可靠聊天服务

**文件：**
- 新增：`src/test/java/org/example/ggbot/ai/ReliableChatServiceTest.java`
- 新增：`src/main/java/org/example/ggbot/ai/ChatFallbackPolicy.java`
- 新增：`src/main/java/org/example/ggbot/ai/ReliableChatService.java`
- 修改：`src/main/java/org/example/ggbot/tool/impl/SummarizeTool.java`

- [ ] **步骤 1：先写失败测试**

```java
package org.example.ggbot.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class ReliableChatServiceTest {

    @Test
    void shouldRetryAndReturnReplyWhenSecondAttemptSucceeds() {
        ChatClient chatClient = mock(ChatClient.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("temporary"))
                .thenReturn("final answer");

        ReliableChatService service = new ReliableChatService(
                Optional.of(chatClient),
                new ChatFallbackPolicy(),
                3,
                Duration.ofMillis(1),
                Duration.ofSeconds(60)
        );

        String reply = service.chat("sys", "hello");

        assertThat(reply).isEqualTo("final answer");
        verify(chatClient, times(2)).prompt();
    }

    @Test
    void shouldReturnFallbackWhenReplyIsBlankAfterAllRetries() {
        ChatClient chatClient = mock(ChatClient.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn(" ")
                .thenReturn("")
                .thenReturn(null);

        ReliableChatService service = new ReliableChatService(
                Optional.of(chatClient),
                new ChatFallbackPolicy(),
                3,
                Duration.ofMillis(1),
                Duration.ofSeconds(60)
        );

        String reply = service.chat("sys", "export chat history");

        assertThat(reply).isEqualTo("抱歉，系统未能理解您的请求，请尝试其他方式提问。");
    }

    @Test
    void shouldReturnRuleFallbackWhenModelThrows() {
        ChatClient chatClient = mock(ChatClient.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("boom"));

        ReliableChatService service = new ReliableChatService(
                Optional.of(chatClient),
                new ChatFallbackPolicy(),
                3,
                Duration.ofMillis(1),
                Duration.ofSeconds(60)
        );

        String reply = service.chat("sys", "导出聊天记录");

        assertThat(reply).contains("导出");
    }
}
```

- [ ] **步骤 2：运行测试，确认它按预期失败**

运行：`mvn -q -s .mvn/local-settings.xml "-Dtest=ReliableChatServiceTest" test`  
预期：由于缺少 `ReliableChatService` / `ChatFallbackPolicy` 而 `FAIL`

- [ ] **步骤 3：写最小实现**

```java
package org.example.ggbot.ai;

public class ChatFallbackPolicy {

    public String resolve(String userPrompt) {
        if (userPrompt != null && userPrompt.contains("导出")) {
            return "当前导出功能暂时不可用，请稍后再试，或换一种方式描述你的导出需求。";
        }
        return "抱歉，系统未能理解您的请求，请尝试其他方式提问。";
    }
}
```

```java
package org.example.ggbot.ai;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

@Slf4j
@RequiredArgsConstructor
public class ReliableChatService {

    private final Optional<ChatClient> chatClient;
    private final ChatFallbackPolicy fallbackPolicy;
    private final int maxAttempts;
    private final Duration retryDelay;
    private final Duration timeout;

    public String chat(String systemPrompt, String userPrompt) {
        if (chatClient.isEmpty()) {
            return fallbackPolicy.resolve(userPrompt);
        }

        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String reply = chatClient.get().prompt()
                        .system(systemPrompt)
                        .user(userPrompt)
                        .call()
                        .content();
                if (reply != null && !reply.isBlank()) {
                    return reply;
                }
            } catch (RuntimeException ex) {
                lastException = ex;
                log.warn("LLM call failed on attempt {}", attempt, ex);
            }

            try {
                Thread.sleep(retryDelay.toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (lastException != null) {
            log.error("LLM call exhausted retries, falling back", lastException);
        }
        return fallbackPolicy.resolve(userPrompt);
    }
}
```

- [ ] **步骤 4：让 `SummarizeTool` 接入可靠聊天服务**

```java
private final ReliableChatService chatService;

private String generateReply(String prompt) {
    return chatService.chat(CHAT_SYSTEM_PROMPT, prompt);
}
```

- [ ] **步骤 5：运行测试，确认通过**

运行：`mvn -q -s .mvn/local-settings.xml "-Dtest=ReliableChatServiceTest" test`  
预期：`PASS`

- [ ] **步骤 6：提交**

```bash
git add src/main/java/org/example/ggbot/ai/ChatFallbackPolicy.java src/main/java/org/example/ggbot/ai/ReliableChatService.java src/main/java/org/example/ggbot/tool/impl/SummarizeTool.java src/test/java/org/example/ggbot/ai/ReliableChatServiceTest.java
git commit -m "feat: add reliable chat fallback layer"
```

### 任务 2：任务模型与存储

**文件：**
- 新增：`src/test/java/org/example/ggbot/job/InMemoryJobServiceTest.java`
- 新增：`src/main/java/org/example/ggbot/job/JobStatus.java`
- 新增：`src/main/java/org/example/ggbot/job/JobRecord.java`
- 新增：`src/main/java/org/example/ggbot/job/JobService.java`
- 新增：`src/main/java/org/example/ggbot/job/InMemoryJobService.java`

- [ ] **步骤 1：先写失败测试**

```java
package org.example.ggbot.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InMemoryJobServiceTest {

    @Test
    void shouldCreateQueuedJobAndUpdateProgress() {
        InMemoryJobService service = new InMemoryJobService();

        JobRecord record = service.create("c1", "u1", "{\"message\":\"hello\"}");
        service.markRunning(record.getJobId(), "正在调用模型");

        JobRecord loaded = service.get(record.getJobId());

        assertThat(loaded.getStatus()).isEqualTo(JobStatus.RUNNING);
        assertThat(loaded.getProgressMessage()).isEqualTo("正在调用模型");
    }

    @Test
    void shouldCreateNewJobWhenRetrying() {
        InMemoryJobService service = new InMemoryJobService();

        JobRecord first = service.create("c1", "u1", "{\"message\":\"hello\"}");
        JobRecord second = service.retry(first.getJobId());

        assertThat(second.getJobId()).isNotEqualTo(first.getJobId());
        assertThat(second.getOriginalRequestPayload()).isEqualTo(first.getOriginalRequestPayload());
    }
}
```

- [ ] **步骤 2：运行测试，确认它按预期失败**

运行：`mvn -q -s .mvn/local-settings.xml "-Dtest=InMemoryJobServiceTest" test`  
预期：由于缺少任务相关类而 `FAIL`

- [ ] **步骤 3：写最小实现**

```java
package org.example.ggbot.job;

public enum JobStatus {
    QUEUED,
    RUNNING,
    RETRYING,
    SUCCEEDED,
    FAILED,
    TIMEOUT
}
```

```java
package org.example.ggbot.job;

import java.time.Instant;
import lombok.Data;

@Data
public class JobRecord {
    private final String jobId;
    private final String conversationId;
    private final String userId;
    private final String originalRequestPayload;
    private JobStatus status = JobStatus.QUEUED;
    private String progressMessage = "已接收请求";
    private int retryCount;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant lastProgressAt = Instant.now();
    private String resultPayload;
    private String errorMessage;
    private String fallbackReply;
}
```

```java
package org.example.ggbot.job;

public interface JobService {
    JobRecord create(String conversationId, String userId, String originalRequestPayload);
    JobRecord get(String jobId);
    void markRunning(String jobId, String progressMessage);
    void markSucceeded(String jobId, String resultPayload);
    void markFailed(String jobId, String errorMessage, String fallbackReply);
    void markTimeout(String jobId, String fallbackReply);
    JobRecord retry(String jobId);
}
```

```java
package org.example.ggbot.job;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class InMemoryJobService implements JobService {

    private final Map<String, JobRecord> jobs = new ConcurrentHashMap<>();

    @Override
    public JobRecord create(String conversationId, String userId, String originalRequestPayload) {
        JobRecord record = new JobRecord("job-" + UUID.randomUUID().toString().replace("-", ""), conversationId, userId, originalRequestPayload);
        jobs.put(record.getJobId(), record);
        return record;
    }

    @Override
    public JobRecord get(String jobId) {
        return jobs.get(jobId);
    }

    @Override
    public void markRunning(String jobId, String progressMessage) {
        JobRecord record = get(jobId);
        record.setStatus(JobStatus.RUNNING);
        record.setProgressMessage(progressMessage);
        record.setUpdatedAt(Instant.now());
        record.setLastProgressAt(Instant.now());
    }

    @Override
    public void markSucceeded(String jobId, String resultPayload) {
        JobRecord record = get(jobId);
        record.setStatus(JobStatus.SUCCEEDED);
        record.setResultPayload(resultPayload);
        record.setUpdatedAt(Instant.now());
        record.setLastProgressAt(Instant.now());
    }

    @Override
    public void markFailed(String jobId, String errorMessage, String fallbackReply) {
        JobRecord record = get(jobId);
        record.setStatus(JobStatus.FAILED);
        record.setErrorMessage(errorMessage);
        record.setFallbackReply(fallbackReply);
        record.setUpdatedAt(Instant.now());
    }

    @Override
    public void markTimeout(String jobId, String fallbackReply) {
        JobRecord record = get(jobId);
        record.setStatus(JobStatus.TIMEOUT);
        record.setFallbackReply(fallbackReply);
        record.setUpdatedAt(Instant.now());
    }

    @Override
    public JobRecord retry(String jobId) {
        JobRecord existing = get(jobId);
        return create(existing.getConversationId(), existing.getUserId(), existing.getOriginalRequestPayload());
    }
}
```

- [ ] **步骤 4：运行测试，确认通过**

运行：`mvn -q -s .mvn/local-settings.xml "-Dtest=InMemoryJobServiceTest" test`  
预期：`PASS`

- [ ] **步骤 5：提交**

```bash
git add src/main/java/org/example/ggbot/job src/test/java/org/example/ggbot/job/InMemoryJobServiceTest.java
git commit -m "feat: add in-memory async job store"
```

### 任务 3：异步判定器与任务 Worker

**文件：**
- 新增：`src/test/java/org/example/ggbot/job/AsyncExecutionDeciderTest.java`
- 新增：`src/test/java/org/example/ggbot/job/JobWorkerTest.java`
- 新增：`src/main/java/org/example/ggbot/job/AsyncExecutionMode.java`
- 新增：`src/main/java/org/example/ggbot/job/AsyncExecutionDecider.java`
- 新增：`src/main/java/org/example/ggbot/job/JobWorker.java`

- [ ] **步骤 1：先写失败测试**

```java
package org.example.ggbot.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentResult;
import org.example.ggbot.agent.AgentService;
import org.junit.jupiter.api.Test;

class AsyncExecutionDeciderTest {

    @Test
    void shouldUseAsyncForExportRequests() {
        AsyncExecutionDecider decider = new AsyncExecutionDecider();
        AgentRequest request = new AgentRequest("c1", "u1", "导出聊天记录", null, null, null, java.util.Map.of());
        assertThat(decider.decide(request)).isEqualTo(AsyncExecutionMode.ASYNC);
    }

    @Test
    void shouldUseSyncForShortChatRequests() {
        AsyncExecutionDecider decider = new AsyncExecutionDecider();
        AgentRequest request = new AgentRequest("c1", "u1", "你好", null, null, null, java.util.Map.of());
        assertThat(decider.decide(request)).isEqualTo(AsyncExecutionMode.SYNC);
    }
}
```

```java
package org.example.ggbot.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentResult;
import org.example.ggbot.agent.AgentService;
import org.example.ggbot.planner.IntentType;
import org.junit.jupiter.api.Test;

class JobWorkerTest {

    @Test
    void shouldMarkJobSucceededWhenAgentCompletes() {
        AgentService agentService = mock(AgentService.class);
        InMemoryJobService jobService = new InMemoryJobService();
        JobRecord record = jobService.create("c1", "u1", "导出聊天记录");

        when(agentService.handle(org.mockito.ArgumentMatchers.any())).thenReturn(
                new AgentResult("task-1", IntentType.CHAT, "最终结果", List.of())
        );

        JobWorker worker = new JobWorker(agentService, jobService);
        worker.process(record.getJobId(), new AgentRequest("c1", "u1", "导出聊天记录", null, null, null, java.util.Map.of()));

        assertThat(jobService.get(record.getJobId()).getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(jobService.get(record.getJobId()).getResultPayload()).contains("最终结果");
    }
}
```

- [ ] **步骤 2：运行测试，确认它按预期失败**

运行：`mvn -q -s .mvn/local-settings.xml "-Dtest=AsyncExecutionDeciderTest,JobWorkerTest" test`  
预期：由于缺少判定器 / worker 类而 `FAIL`

- [ ] **步骤 3：写最小实现**

```java
package org.example.ggbot.job;

public enum AsyncExecutionMode {
    SYNC,
    ASYNC
}
```

```java
package org.example.ggbot.job;

import org.example.ggbot.agent.AgentRequest;
import org.springframework.stereotype.Component;

@Component
public class AsyncExecutionDecider {

    public AsyncExecutionMode decide(AgentRequest request) {
        String input = request.getUserInput() == null ? "" : request.getUserInput();
        if (input.contains("导出") || input.contains("文档") || input.contains("PPT") || input.contains("汇报")) {
            return AsyncExecutionMode.ASYNC;
        }
        return AsyncExecutionMode.SYNC;
    }
}
```

```java
package org.example.ggbot.job;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.agent.AgentResult;
import org.example.ggbot.agent.AgentService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobWorker {

    private final AgentService agentService;
    private final JobService jobService;

    public void process(String jobId, AgentRequest request) {
        jobService.markRunning(jobId, "正在调用模型");
        try {
            AgentResult result = agentService.handle(request);
            jobService.markSucceeded(jobId, result.getReplyText());
        } catch (RuntimeException ex) {
            jobService.markFailed(jobId, ex.getMessage(), "请求处理失败，请稍后再试。");
        }
    }
}
```

- [ ] **步骤 4：运行测试，确认通过**

运行：`mvn -q -s .mvn/local-settings.xml "-Dtest=AsyncExecutionDeciderTest,JobWorkerTest" test`  
预期：`PASS`

- [ ] **步骤 5：提交**

```bash
git add src/main/java/org/example/ggbot/job/AsyncExecutionMode.java src/main/java/org/example/ggbot/job/AsyncExecutionDecider.java src/main/java/org/example/ggbot/job/JobWorker.java src/test/java/org/example/ggbot/job/AsyncExecutionDeciderTest.java src/test/java/org/example/ggbot/job/JobWorkerTest.java
git commit -m "feat: add async execution decider and job worker"
```

### 任务 4：Watchdog 与 Web 任务接口

**文件：**
- 新增：`src/test/java/org/example/ggbot/job/JobWatchdogTest.java`
- 新增：`src/test/java/org/example/ggbot/adapter/web/WebJobControllerTest.java`
- 新增：`src/main/java/org/example/ggbot/job/JobWatchdog.java`
- 新增：`src/main/java/org/example/ggbot/adapter/web/dto/WebChatAcceptedResponse.java`
- 新增：`src/main/java/org/example/ggbot/adapter/web/dto/WebJobStatusResponse.java`
- 修改：`src/main/java/org/example/ggbot/adapter/web/WebAgentController.java`

- [ ] **步骤 1：先写失败测试**

```java
package org.example.ggbot.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class JobWatchdogTest {

    @Test
    void shouldMarkRunningJobTimedOutWhenNoProgress() {
        InMemoryJobService service = new InMemoryJobService();
        JobRecord record = service.create("c1", "u1", "导出聊天记录");
        service.markRunning(record.getJobId(), "正在调用模型");
        service.get(record.getJobId()).setLastProgressAt(Instant.now().minusSeconds(120));

        JobWatchdog watchdog = new JobWatchdog(service, java.time.Duration.ofSeconds(60));
        watchdog.scan();

        assertThat(service.get(record.getJobId()).getStatus()).isEqualTo(JobStatus.TIMEOUT);
    }
}
```

- [ ] **步骤 2：运行测试，确认它按预期失败**

运行：`mvn -q -s .mvn/local-settings.xml "-Dtest=JobWatchdogTest" test`  
预期：由于缺少 watchdog 类而 `FAIL`

- [ ] **步骤 3：写最小实现**

```java
package org.example.ggbot.job;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobWatchdog {

    private final JobService jobService;
    private final Duration timeout;

    @Scheduled(fixedDelay = 10000)
    public void scan() {
        if (!(jobService instanceof InMemoryJobService inMemory)) {
            return;
        }
        for (JobRecord record : inMemory.snapshot()) {
            if ((record.getStatus() == JobStatus.QUEUED || record.getStatus() == JobStatus.RUNNING || record.getStatus() == JobStatus.RETRYING)
                    && record.getLastProgressAt().isBefore(Instant.now().minus(timeout))) {
                jobService.markTimeout(record.getJobId(), "我们遇到了一些问题，稍后再试一次。");
            }
        }
    }
}
```

- [ ] **步骤 4：给 `InMemoryJobService` 增加快照能力**

```java
public Collection<JobRecord> snapshot() {
    return List.copyOf(jobs.values());
}
```

- [ ] **步骤 5：增加 accepted/status DTO 和接口**

```java
public record WebChatAcceptedResponse(boolean accepted, String jobId, String status) {}
```

```java
public record WebJobStatusResponse(String jobId, String status, String progressMessage, boolean canRetry, String replyText, String fallbackReply) {}
```

- [ ] **步骤 6：运行测试，确认通过**

运行：`mvn -q -s .mvn/local-settings.xml "-Dtest=JobWatchdogTest,WebJobControllerTest" test`  
预期：`PASS`

- [ ] **步骤 7：提交**

```bash
git add src/main/java/org/example/ggbot/job/JobWatchdog.java src/main/java/org/example/ggbot/adapter/web src/test/java/org/example/ggbot/job/JobWatchdogTest.java src/test/java/org/example/ggbot/adapter/web/WebJobControllerTest.java
git commit -m "feat: add job polling and watchdog endpoints"
```

### 任务 5：前端忙碌态与重试交互

**文件：**
- 修改：`src/main/resources/static/index.html`
- 修改：`src/main/resources/static/app.js`
- 修改：`src/main/resources/static/app.css`

- [ ] **步骤 1：写手工验收清单**

```text
1. 同步短请求发送后按钮变成圈圈且不可点击
2. 聊天区显示“正在生成中...”
3. 响应完成后提示消失并显示最终结果
4. 异步任务提交后显示“任务已提交，正在处理中...”
5. 轮询过程中进度文案会更新
6. 失败时显示错误提示和“重传请求”按钮
```

- [ ] **步骤 2：在 `index.html` 中增加加载态与重试挂点**

```html
<button id="send-button" type="submit">
  <span class="button-label">发送请求</span>
  <span class="spinner hidden" aria-hidden="true"></span>
</button>
```

- [ ] **步骤 3：增加最小 CSS**

```css
.spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.35);
  border-top-color: #fff;
  border-radius: 50%;
  display: inline-block;
  animation: spin 0.8s linear infinite;
}

.hidden {
  display: none;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
```

- [ ] **步骤 4：在 `app.js` 中增加轮询与重试流程**

```javascript
let activeJobId = null;
let lastSubmittedMessage = "";

function setBusyState(isBusy, label = "发送请求") {
    sendButton.disabled = isBusy;
    messageInput.disabled = isBusy;
    sendButton.querySelector(".button-label").textContent = label;
    sendButton.querySelector(".spinner").classList.toggle("hidden", !isBusy);
}
```

- [ ] **步骤 5：执行手工验证**

运行：`mvn -q -s .mvn/local-settings.xml test`  
预期：`PASS`，然后启动应用并在浏览器中逐项验证上面的清单

- [ ] **步骤 6：提交**

```bash
git add src/main/resources/static/index.html src/main/resources/static/app.js src/main/resources/static/app.css
git commit -m "feat: add frontend loading state and job retry flow"
```

## 自检

- 规格覆盖：可靠模型兜底、混合同步/异步分发、任务状态轮询、watchdog 超时、重试流程和前端体验，均已由任务 1 到 5 覆盖。
- 占位符检查：没有残留 `TODO` / `TBD` 占位内容；步骤中包含精确的文件和命令。
- 类型一致性：共享命名保持一致，包括 `JobStatus`、`JobRecord`、`JobService`、`AsyncExecutionDecider`、`JobWorker`、`ReliableChatService`。
