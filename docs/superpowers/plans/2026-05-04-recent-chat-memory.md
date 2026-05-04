# Recent Chat Memory Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add recent 5-round chat memory for ordinary chat prompts without changing the existing low-level chat service interface.

**Architecture:** Keep `ReliableChatService` as the raw LLM transport layer, add `MemoryManager` for prompt assembly and memory writes, and add `ContextAwareChatService` as the single context-aware entry for tool-level chat calls. `SummarizeTool` becomes a thin adapter that delegates prompt assembly and memory updates to this service.

**Tech Stack:** Java 21, Spring Boot, Reactor, JUnit 5, Mockito

---

### Task 1: Make AgentContext history writable for memory updates

**Files:**
- Modify: `src/main/java/org/example/ggbot/agent/AgentContext.java`
- Test: `src/test/java/org/example/ggbot/tool/impl/SummarizeToolTest.java`

- [ ] Add a constructor that copies `conversationHistory` into a mutable `ArrayList`
- [ ] Add a small helper like `addConversationMessage(String message)` for controlled writes

### Task 2: Add MemoryManager with recent 5-round logic

**Files:**
- Create: `src/main/java/org/example/ggbot/ai/MemoryManager.java`
- Test: `src/test/java/org/example/ggbot/ai/MemoryManagerTest.java`

- [ ] Write tests for prompt building and recent message truncation
- [ ] Implement `buildPrompt`, `addUserMessage`, `addAssistantMessage`, `getRecentMessages`, `formatRecentMessages`
- [ ] Use a `RECENT_ROUNDS = 5` constant

### Task 3: Add ContextAwareChatService

**Files:**
- Create: `src/main/java/org/example/ggbot/ai/ContextAwareChatService.java`
- Test: `src/test/java/org/example/ggbot/ai/ContextAwareChatServiceTest.java`

- [ ] Write tests for `chat` prompt assembly and memory write-back
- [ ] Write tests for `stream` prompt assembly and completion-time memory write-back
- [ ] Implement wrapper methods delegating to `ReliableChatService`

### Task 4: Switch SummarizeTool to the new context-aware entry

**Files:**
- Modify: `src/main/java/org/example/ggbot/tool/impl/SummarizeTool.java`
- Test: `src/test/java/org/example/ggbot/tool/impl/SummarizeToolTest.java`

- [ ] Replace direct `ReliableChatService` dependency with `ContextAwareChatService`
- [ ] Keep template fallback behavior unchanged
- [ ] Verify streaming still forwards chunks to the existing consumer

### Task 5: Run focused verification

**Files:**
- Test: `src/test/java/org/example/ggbot/ai/MemoryManagerTest.java`
- Test: `src/test/java/org/example/ggbot/ai/ContextAwareChatServiceTest.java`
- Test: `src/test/java/org/example/ggbot/tool/impl/SummarizeToolTest.java`

- [ ] Run `mvn -q "-Dtest=MemoryManagerTest,ContextAwareChatServiceTest,SummarizeToolTest" test`
- [ ] Fix any regressions
