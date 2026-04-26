# Web 页面优化与 Prompt 集中管理 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 优化现有 Web 页面视觉与文案，移除 `MVP` 展示痕迹，并将长文本 `system prompt` 统一迁移到 `src/main/resources/prompts/` 下集中管理。

**Architecture:** 保持现有静态前端结构和后端接口不变，只调整 `index.html`、`app.css`、`app.js` 的展示层内容。后端新增一个基于 classpath 的 prompt 读取组件，由 `SummarizeTool` 通过依赖注入读取资源文件而不是内联多行字符串。

**Tech Stack:** Spring Boot, Spring MVC, JUnit 5, Mockito, AssertJ, 静态 HTML/CSS/JavaScript

---

## File Map

- Modify: `src/main/resources/static/index.html`
- Modify: `src/main/resources/static/app.css`
- Modify: `src/main/resources/static/app.js`
- Create: `src/main/resources/prompts/summarize-system-prompt.txt`
- Create: `src/main/java/org/example/ggbot/prompt/ClasspathPromptRepository.java`
- Modify: `src/main/java/org/example/ggbot/tool/impl/SummarizeTool.java`
- Modify: `src/main/java/org/example/ggbot/adapter/web/WebAgentController.java`
- Modify: `src/test/java/org/example/ggbot/AgentPilotApplicationTests.java`
- Modify: `src/test/java/org/example/ggbot/tool/impl/SummarizeToolTest.java`
- Create: `src/test/java/org/example/ggbot/prompt/ClasspathPromptRepositoryTest.java`

### Task 1: 调整首页文案与视觉样式

**Files:**
- Modify: `src/main/resources/static/index.html`
- Modify: `src/main/resources/static/app.css`
- Modify: `src/main/resources/static/app.js`
- Modify: `src/main/java/org/example/ggbot/adapter/web/WebAgentController.java`
- Test: `src/test/java/org/example/ggbot/AgentPilotApplicationTests.java`

- [ ] **Step 1: 为首页文案变更写失败测试**

```java
@Test
void shouldServeWebHomePageWithoutMvpBranding() throws Exception {
    mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("index.html"));

    mockMvc.perform(get("/index.html"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("GGbot 工作台")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("web-console-session")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("MVP"))))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/health")));
}
```

- [ ] **Step 2: 运行首页测试并确认失败**

Run: `mvn -Dtest=AgentPilotApplicationTests#shouldServeWebHomePageWithoutMvpBranding test`

Expected: FAIL，因为当前页面仍包含 `GGbot MVP` 和 `web-mvp-session`

- [ ] **Step 3: 修改静态页面与默认值**

```html
<title>GGbot 工作台</title>
...
<p class="eyebrow">GGbot Workspace</p>
<h1>面向文档、汇报与通用问答的智能工作台</h1>
<p class="hero-copy">
    在一个页面里完成对话、任务跟踪和结果查看，直接连接当前 Spring Boot Agent 服务。
</p>
...
<input id="conversation-id" type="text" value="web-console-session">
```

```javascript
body: JSON.stringify({
    conversationId: conversationIdInput.value || "web-console-session",
    userId: userIdInput.value || "demo-user",
    message
})
```

```java
request.setConversationId((String) payload.getOrDefault("conversationId", "web-console-session"));
```

- [ ] **Step 4: 重做页面样式但不改交互结构**

```css
:root {
    --bg: #f3eadf;
    --surface: rgba(255, 252, 247, 0.92);
    --surface-strong: #fffdf9;
    --line: #d6c4ad;
    --text: #1f1a15;
    --muted: #675a4c;
    --brand: #155e75;
    --brand-strong: #103f4d;
    --accent: #b45309;
    --shadow: 0 24px 60px rgba(58, 40, 24, 0.12);
}

.hero {
    grid-template-columns: 1.8fr 0.8fr;
    align-items: end;
}

.panel,
.status-card {
    background: var(--surface);
    border: 1px solid rgba(214, 196, 173, 0.85);
    box-shadow: var(--shadow);
}

.chat-feed {
    min-height: 460px;
}
```

- [ ] **Step 5: 更新能力说明与默认提示文案**

```html
<div class="message-body">
    这里会显示对话内容。可以先输入一个需求，例如“帮我整理一个产品 PRD 文档”。
</div>
...
<ul>
    <li>对话请求与任务执行统一入口</li>
    <li>文档、PPT、总结类结果快速返回</li>
    <li>异步任务状态轮询与失败重试</li>
</ul>
```

- [ ] **Step 6: 重新运行首页测试并确认通过**

Run: `mvn -Dtest=AgentPilotApplicationTests#shouldServeWebHomePageWithoutMvpBranding test`

Expected: PASS

- [ ] **Step 7: 提交前端页面改动**

```bash
git add src/main/resources/static/index.html src/main/resources/static/app.css src/main/resources/static/app.js src/main/java/org/example/ggbot/adapter/web/WebAgentController.java src/test/java/org/example/ggbot/AgentPilotApplicationTests.java
git commit -m "feat: refresh web console copy and styling"
```

### Task 2: 抽离 SummarizeTool 的 system prompt 到 prompts 目录

**Files:**
- Create: `src/main/resources/prompts/summarize-system-prompt.txt`
- Create: `src/main/java/org/example/ggbot/prompt/ClasspathPromptRepository.java`
- Modify: `src/main/java/org/example/ggbot/tool/impl/SummarizeTool.java`
- Modify: `src/test/java/org/example/ggbot/tool/impl/SummarizeToolTest.java`
- Create: `src/test/java/org/example/ggbot/prompt/ClasspathPromptRepositoryTest.java`

- [ ] **Step 1: 为 prompt 读取组件写失败测试**

```java
class ClasspathPromptRepositoryTest {

    @Test
    void shouldLoadPromptTextFromClasspath() {
        ClasspathPromptRepository repository = new ClasspathPromptRepository();

        String prompt = repository.load("summarize-system-prompt.txt");

        assertThat(prompt).contains("你是 GGbot 的 Web 对话助手。");
        assertThat(prompt).doesNotContain("MVP");
    }

    @Test
    void shouldFailFastWhenPromptResourceDoesNotExist() {
        ClasspathPromptRepository repository = new ClasspathPromptRepository();

        assertThatThrownBy(() -> repository.load("missing.txt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing.txt");
    }
}
```

- [ ] **Step 2: 为 SummarizeTool 改造写失败测试**

```java
@Test
void shouldDelegateToReliableChatServiceWithPromptFromRepositoryWhenAvailable() {
    ReliableChatService chatService = mock(ReliableChatService.class);
    ClasspathPromptRepository repository = mock(ClasspathPromptRepository.class);
    when(chatService.isAvailable()).thenReturn(true);
    when(repository.load("summarize-system-prompt.txt")).thenReturn("""
            你是 GGbot 的 Web 对话助手。
            你的回答应当直接、简洁、可执行。
            如果用户只是普通聊天或提问，请直接回答，不要假装生成文档或 PPT。
            """);
    when(chatService.chat(anyString(), eq("你好，请介绍一下你自己"))).thenReturn("这是模型回复");

    SummarizeTool tool = new SummarizeTool(chatService, repository);

    ToolResult result = tool.execute("你好，请介绍一下你自己", context(), Map.of());

    assertThat(result.getSummary()).isEqualTo("这是模型回复");
    verify(repository).load("summarize-system-prompt.txt");
    verify(chatService).chat("""
            你是 GGbot 的 Web 对话助手。
            你的回答应当直接、简洁、可执行。
            如果用户只是普通聊天或提问，请直接回答，不要假装生成文档或 PPT。
            """, "你好，请介绍一下你自己");
}
```

- [ ] **Step 3: 运行摘要工具与 prompt 仓库测试并确认失败**

Run: `mvn -Dtest=ClasspathPromptRepositoryTest,SummarizeToolTest test`

Expected: FAIL，因为仓库组件和新的构造函数尚不存在

- [ ] **Step 4: 创建 prompt 资源文件与读取组件**

```text
你是 GGbot 的 Web 对话助手。
你的回答应当直接、简洁、可执行。
如果用户只是普通聊天或提问，请直接回答，不要假装生成文档或 PPT。
```

```java
@Component
public class ClasspathPromptRepository {

    public String load(String name) {
        ClassPathResource resource = new ClassPathResource("prompts/" + name);
        if (!resource.exists()) {
            throw new IllegalStateException("Prompt resource not found: " + name);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load prompt resource: " + name, exception);
        }
    }
}
```

- [ ] **Step 5: 修改 SummarizeTool 使用集中 prompt**

```java
@Component
@Data
public class SummarizeTool {

    private static final String SYSTEM_PROMPT_NAME = "summarize-system-prompt.txt";

    private final ReliableChatService chatService;
    private final ClasspathPromptRepository promptRepository;

    public SummarizeTool(ReliableChatService chatService, ClasspathPromptRepository promptRepository) {
        this.chatService = chatService;
        this.promptRepository = promptRepository;
    }

    private String generateReply(String prompt) {
        if (!chatService.isAvailable()) {
            return templateReply(prompt);
        }
        return chatService.chat(promptRepository.load(SYSTEM_PROMPT_NAME), prompt);
    }
}
```

- [ ] **Step 6: 补齐与更新测试断言**

```java
assertThat(result.getSummary()).contains("已收到你的需求：帮我总结一下");
verify(chatService).isAvailable();
verifyNoMoreInteractions(chatService);
```

```java
assertThatThrownBy(() -> repository.load("missing.txt"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Prompt resource not found");
```

- [ ] **Step 7: 运行相关测试并确认通过**

Run: `mvn -Dtest=ClasspathPromptRepositoryTest,SummarizeToolTest,AgentPilotApplicationTests test`

Expected: PASS

- [ ] **Step 8: 提交 prompt 集中化改动**

```bash
git add src/main/resources/prompts/summarize-system-prompt.txt src/main/java/org/example/ggbot/prompt/ClasspathPromptRepository.java src/main/java/org/example/ggbot/tool/impl/SummarizeTool.java src/test/java/org/example/ggbot/prompt/ClasspathPromptRepositoryTest.java src/test/java/org/example/ggbot/tool/impl/SummarizeToolTest.java src/test/java/org/example/ggbot/AgentPilotApplicationTests.java
git commit -m "refactor: centralize summarize system prompt"
```

## Self-Review

- 规格覆盖检查：前端去 `MVP`、页面视觉优化、默认会话 ID 调整、`system prompt` 集中管理、资源缺失快速失败、摘要工具保持兜底逻辑，均已在 Task 1 和 Task 2 覆盖。
- 占位检查：计划中没有 `TODO`、`TBD`、`适当处理` 这类占位描述。
- 类型一致性检查：统一使用 `ClasspathPromptRepository#load(String name)` 和 `summarize-system-prompt.txt`，与 `SummarizeTool` 的构造参数保持一致。
