# Web Frontend Conversations Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the web frontend from the in-memory `/api/sessions` model to the persistent `/api/conversations` model backed by MySQL.

**Architecture:** Add a lightweight web context endpoint that exposes the current web user's `personalOrgId`, update the streaming chat endpoint to create/persist conversations when `conversationId` is absent and emit the real `conversationId` immediately, then switch the frontend list/detail/send flow to `/api/conversations`.

**Tech Stack:** Spring Boot MVC, JPA/MySQL, SSE, static React frontend.

---

### Task 1: Expose current web context for the frontend

**Files:**
- Create: `src/main/java/org/example/ggbot/adapter/web/WebContextController.java`
- Create: `src/main/java/org/example/ggbot/adapter/web/dto/WebContextResponse.java`
- Test: `src/test/java/org/example/ggbot/adapter/web/WebContextControllerTest.java`

- [ ] Add a controller that resolves `web_user_key` via `IdentityService` and returns `{ userId, personalOrgId }`.
- [ ] Add a controller test covering the happy path with a cookie.

### Task 2: Make streaming chat return/create persistent conversations

**Files:**
- Modify: `src/main/java/org/example/ggbot/adapter/web/WebAgentController.java`
- Modify: `src/main/java/org/example/ggbot/adapter/web/dto/WebChatRequest.java`
- Test: `src/test/java/org/example/ggbot/adapter/web/WebAgentControllerTest.java`

- [ ] Add failing tests for:
  - creating a new persistent conversation when `conversationId` is missing
  - reusing an existing persistent conversation when `conversationId` is present
- [ ] Update the controller to resolve the current web user and personal org, create/persist through `ConversationService`, and emit an initial SSE event containing `conversationId`.

### Task 3: Provide frontend-shaped conversation detail through `/api/conversations`

**Files:**
- Modify: `src/test/java/org/example/ggbot/adapter/web/ConversationControllerTest.java`
- Modify: `src/main/java/org/example/ggbot/adapter/web/ConversationController.java`

- [ ] Extend tests so the frontend can obtain conversation summaries and message lists with the fields it already needs.
- [ ] Keep responses compatible with the frontend migration target.

### Task 4: Switch frontend data access to conversations

**Files:**
- Modify: `src/main/resources/static/services/session-api.js`
- Modify: `src/main/resources/static/app.js`
- Test: `src/test/frontend/app-merge-session.test.mjs`

- [ ] Replace `/api/sessions` usage with:
  - web context bootstrap
  - `/api/conversations`
  - `/api/conversations/{id}/messages`
- [ ] Remove explicit `createSession()` from first-send flow.
- [ ] Update the SSE handling to jump as soon as the first event yields `conversationId`.

### Task 5: Remove debug-only UI and verify end-to-end behavior

**Files:**
- Modify: `src/main/resources/static/app.js`
- Modify: `src/main/resources/static/app.css`

- [ ] Remove the temporary debug panel once the migration is stable.
- [ ] Run targeted tests and a manual browser verification checklist.
