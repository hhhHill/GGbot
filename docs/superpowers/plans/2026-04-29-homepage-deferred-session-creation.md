# Homepage Deferred Session Creation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep the sidebar history visible on the main page, but create and select a chat session only when the user sends the first message.

**Architecture:** The route layer treats `/` and legacy `/chat/new` as the same home state: no active session selected, no session fetch, no session creation. The chat composer owns session creation on demand, and the sidebar "new chat" action simply returns to the home route.

**Tech Stack:** React hooks, browser History API, existing static chat app, Spring Boot backend.

---

### Task 1: Make the home route non-destructive

**Files:**
- Modify: `src/main/resources/static/app.js`
- Test: manual browser navigation and existing Maven test suite

- [ ] **Step 1: Update the route bootstrap and route handler**

```js
const initialRoutePath = window.location.pathname;
const initialRoute = getChatRoute(initialRoutePath);

const [state, dispatch] = useReducer(reducer, {
    sessions: cachedState.sessions || [],
    activeSessionId: initialRoute.type === "session" ? cachedState.activeSessionId || null : null,
    activeSession: initialRoute.type === "session" ? cachedState.activeSession || null : null,
    loadingSessions: false,
    loadingSession: false,
    sending: false,
    error: "",
    userId: DEFAULT_USER_ID,
    draft: cachedState.draft || ""
});

async function openRoute(pathname) {
    const route = getChatRoute(pathname);
    if (route.type === "root" || route.type === "new") {
        dispatch({ type: "session/cleared" });
        if (route.type === "new") {
            goToHome(true);
        }
        return;
    }
    if (route.type === "session") {
        await loadSessionById(route.sessionId);
    }
}
```

- [ ] **Step 2: Run the app and verify home shows only the sidebar history plus an empty chat state**

Run: `mvn spring-boot:run`
Expected: opening `/` shows the sidebar history, with no session selected and no new session created.

- [ ] **Step 3: Verify the old `/chat/new` path redirects back to `/` without creating a session**

Run: open `/chat/new` in the browser
Expected: the URL becomes `/`, the sidebar stays visible, and no session is created yet.

### Task 2: Create the session only when the first message is sent

**Files:**
- Modify: `src/main/resources/static/app.js`

- [ ] **Step 1: Keep `handleSendMessage` responsible for on-demand creation**

```js
async function handleSendMessage(content) {
    const message = content.trim();
    if (!message) {
        return;
    }

    let sessionId = state.activeSessionId;
    if (!sessionId) {
        const session = await createSession(state.userId);
        dispatch({ type: "session/loaded", session });
        replaceWithSession(session.sessionId);
        sessionId = session.sessionId;
    }

    // continue existing stream submission flow
}
```

- [ ] **Step 2: Keep the current optimistic message flow intact**

```js
dispatch({
    type: "message/optimistic",
    userMessage: { role: "user", content: message, timestamp, clientId: `user-${timestamp}` },
    pendingMessage: {
        role: "assistant",
        content: "任务已创建，正在执行中...",
        timestamp: timestamp + 1,
        clientId: pendingId,
        pending: true
    }
});
```

- [ ] **Step 3: Run the targeted test suite**

Run: `mvn -q -Dtest=AgentTaskExecutorTest test`
Expected: pass, confirming the backend stream path still behaves normally after the frontend route change.

### Task 3: Make the sidebar "new chat" return home

**Files:**
- Modify: `src/main/resources/static/app.js`
- Modify: `src/main/resources/static/services/router.js`

- [ ] **Step 1: Add a home navigation helper and route the button to it**

```js
export function goToHome(replace = false) {
    navigate("/", replace);
}
```

```js
<${Sidebar}
    ...
    onCreateSession={() => goToHome(true)}
    ...
/>
```

- [ ] **Step 2: Keep the home page from reselecting the last active session**

```js
useEffect(() => {
    if (route.type === "root" || route.type === "new") {
        dispatch({ type: "session/cleared" });
    }
}, [routePath]);
```

- [ ] **Step 3: Manual verification**

Run: click "新建会话" from any session
Expected: the app returns to the main page, the sidebar stays visible, and no session is selected.
