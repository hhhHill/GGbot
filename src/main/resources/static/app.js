import React, { useEffect, useMemo, useReducer, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import { html } from "./lib/html.js";
import { Sidebar } from "./components/Sidebar.js";
import { HomePage } from "./components/HomePage.js";
import { ConversationPage } from "./components/ConversationPage.js";
import { ChatLayout } from "./components/ChatLayout.js";
import {
    createFeishuBindToken,
    deleteSession,
    fetchOrganizations,
    fetchSession,
    fetchSessions,
    fetchTaskStatus,
    fetchWebContext,
    loginLocalAccount,
    logoutLocalAccount,
    openAgentChatStream,
    registerLocalAccount,
    renameSession,
    retryTask,
    switchOrganization
} from "./services/session-api.js";
import {
    getChatRoute,
    goToHome,
    goToSession,
    replaceWithSession,
    subscribeRouteChange
} from "./services/router.js";
import {
    loadCachedChatState,
    loadDebugPanelCollapsedPreference,
    loadSidebarCollapsedPreference,
    saveCachedChatState,
    saveDebugPanelCollapsedPreference,
    saveSidebarCollapsedPreference
} from "./services/local-storage.js";

const cachedState = loadCachedChatState();
const initialRoutePath = window.location.pathname;
const initialRoute = getChatRoute(initialRoutePath);

function reducer(state, action) {
    switch (action.type) {
        case "sessions/loading":
            return { ...state, loadingSessions: true };
        case "sessions/loaded":
            return {
                ...state,
                loadingSessions: false,
                sessions: action.sessions,
                activeSession: syncSessionTitle(state.activeSession, action.sessions)
            };
        case "session/renamed":
            return {
                ...state,
                sessions: state.sessions.map((session) =>
                    session.sessionId === action.session.sessionId ? { ...session, ...action.session } : session
                ),
                activeSession: state.activeSession?.sessionId === action.session.sessionId
                    ? { ...state.activeSession, title: action.session.title, updatedAt: action.session.updatedAt }
                    : state.activeSession
            };
        case "session/deleted":
            return {
                ...state,
                sessions: state.sessions.filter((session) => session.sessionId !== action.sessionId),
                activeSessionId: state.activeSessionId === action.sessionId ? null : state.activeSessionId,
                activeSession: state.activeSession?.sessionId === action.sessionId ? null : state.activeSession,
                sending: state.activeSession?.sessionId === action.sessionId ? false : state.sending
            };
        case "session/loading":
            return { ...state, loadingSession: true, activeSessionId: action.sessionId ?? state.activeSessionId };
        case "session/loaded":
            const mergedSession = mergeSession(action.session, state.activeSession);
            return {
                ...state,
                loadingSession: false,
                activeSessionId: mergedSession.sessionId,
                activeSession: mergedSession
            };
        case "session/identified":
            return identifySession(state, action.sessionId);
        case "session/cleared":
            return {
                ...state,
                loadingSession: false,
                activeSessionId: null,
                activeSession: null,
                sending: false,
                error: ""
            };
        case "session/error":
            return { ...state, loadingSession: false, sending: false, error: action.message };
        case "message/optimistic":
            const optimisticTimestamp = action.timestamp ?? Date.now();
            const optimisticSession = state.activeSession || {
                sessionId: action.sessionId ?? state.activeSessionId ?? null,
                title: action.title || "新对话",
                messages: [],
                createdAt: new Date(optimisticTimestamp).toISOString(),
                updatedAt: new Date(optimisticTimestamp).toISOString()
            };
            return {
                ...state,
                activeSessionId: optimisticSession.sessionId,
                sending: true,
                error: "",
                activeSession: {
                    ...optimisticSession,
                    title: optimisticSession.title || action.title || "新对话",
                    updatedAt: new Date(optimisticTimestamp).toISOString(),
                    messages: [...optimisticSession.messages, action.userMessage, action.pendingMessage]
                }
            };
        case "message/replace-pending":
            const replacedSession = state.activeSession || {
                sessionId: state.activeSessionId,
                title: "新对话",
                messages: []
            };
            return {
                ...state,
                sending: action.keepSending ?? false,
                activeSession: {
                    ...replacedSession,
                    messages: replacedSession.messages.map((message) =>
                        message.clientId === action.clientId ? action.message : message
                    )
                }
            };
        case "message/task-update":
            if (!state.activeSession) {
                return state;
            }
            return {
                ...state,
                activeSession: {
                    ...state.activeSession,
                    messages: state.activeSession.messages.map((message) =>
                        message.taskId === action.taskId ? { ...message, ...action.patch } : message
                    )
                }
            };
        case "message/append-chunk":
            if (!state.activeSession) {
                return state;
            }
            return {
                ...state,
                activeSession: {
                    ...state.activeSession,
                    messages: state.activeSession.messages.map((message) => {
                        if (message.clientId !== action.clientId) {
                            return message;
                        }
                        const placeholderPrefixes = ["任务已创建", "任务执行中", "node="];
                        const shouldReplace = !message.content || placeholderPrefixes.some((prefix) => message.content.startsWith(prefix));
                        const nextContent = shouldReplace ? action.chunk : `${message.content}${action.chunk}`;
                        return {
                            ...message,
                            content: nextContent,
                            pending: true
                        };
                    })
                }
            };
        case "draft/set":
            return { ...state, draft: action.value };
        case "voice/state":
            return { ...state, voiceState: action.value, error: action.error ?? state.error };
        case "voice/mode":
            return { ...state, voiceMode: action.value };
        case "voice/result":
            return { ...state, voiceState: "idle", draft: action.text, error: "" };
        case "sending":
            return { ...state, sending: action.value };
        case "error":
            return { ...state, error: action.message, sending: false };
        default:
            return state;
    }
}

function App() {
    const shouldRestoreSession =
        initialRoute.type === "session" &&
        cachedState.activeSessionId === initialRoute.sessionId &&
        cachedState.activeSession?.sessionId === initialRoute.sessionId;
    const [state, dispatch] = useReducer(reducer, {
        sessions: cachedState.sessions || [],
        activeSessionId: shouldRestoreSession ? cachedState.activeSessionId || null : null,
        activeSession: shouldRestoreSession ? cachedState.activeSession || null : null,
        loadingSessions: false,
        loadingSession: false,
        sending: false,
        error: "",
        draft: cachedState.draft || "",
        voiceState: "idle",
        voiceMode: "fill"
    });
    const [routePath, setRoutePath] = useState(window.location.pathname);
    const [sidebarCollapsed, setSidebarCollapsed] = useState(() => loadSidebarCollapsedPreference());
    const [debugPanelCollapsed, setDebugPanelCollapsed] = useState(() => loadDebugPanelCollapsedPreference());
    const [selectedSessionId, setSelectedSessionId] = useState(
        shouldRestoreSession ? cachedState.activeSessionId || null : initialRoute.type === "session" ? initialRoute.sessionId : null
    );
    const [webContext, setWebContext] = useState(null);
    const [authDraft, setAuthDraft] = useState({ username: "", password: "" });
    const [authBusy, setAuthBusy] = useState(false);
    const [organizations, setOrganizations] = useState([]);
    const [currentOrgId, setCurrentOrgId] = useState(null);
    const [switchingOrg, setSwitchingOrg] = useState(false);
    const [bindingToken, setBindingToken] = useState("");
    const [bindingTokenLoading, setBindingTokenLoading] = useState(false);
    const [contextReady, setContextReady] = useState(false);
    const pollingRef = useRef(new Map());
    const chatStreamRef = useRef(null);

    useEffect(() => subscribeRouteChange((path) => setRoutePath(path)), []);

    useEffect(() => {
        let active = true;
        async function bootstrap() {
            try {
                const context = await fetchWebContext();
                if (!active) {
                    return;
                }
                setWebContext(context);
                setCurrentOrgId(context.personalOrgId ? String(context.personalOrgId) : null);
                setContextReady(true);
            } catch (error) {
                if (!active) {
                    return;
                }
                dispatch({ type: "error", message: error.message || "初始化会话上下文失败" });
                setContextReady(true);
            }
        }
        void bootstrap();
        return () => {
            active = false;
        };
    }, []);

    useEffect(() => {
        if (!contextReady || !webContext) {
            return;
        }
        void loadOrganizations();
    }, [contextReady, webContext]);

    useEffect(() => {
        if (!contextReady || !currentOrgId) {
            return;
        }
        void loadSessions(currentOrgId);
    }, [contextReady, currentOrgId]);

    useEffect(() => {
        if (!contextReady || !currentOrgId) {
            return;
        }
        void openRoute(routePath, currentOrgId);
    }, [contextReady, routePath, currentOrgId, state.sessions]);

    useEffect(() => {
        saveCachedChatState({
            sessions: state.sessions,
            activeSessionId: state.activeSessionId,
            activeSession: state.activeSession,
            draft: state.draft
        });
    }, [state.sessions, state.activeSessionId, state.activeSession, state.draft]);

    useEffect(() => {
        saveSidebarCollapsedPreference(sidebarCollapsed);
    }, [sidebarCollapsed]);

    useEffect(() => {
        saveDebugPanelCollapsedPreference(debugPanelCollapsed);
    }, [debugPanelCollapsed]);

    useEffect(() => {
        const messages = state.activeSession?.messages || [];
        const activeTaskIds = new Set();
        messages.forEach((message) => {
            if (message.taskId && !isFinalTaskStatus(message.taskStatus)) {
                activeTaskIds.add(message.taskId);
                ensureTaskPolling(message.taskId);
            }
        });
        for (const [taskId, timeoutId] of pollingRef.current.entries()) {
            if (!activeTaskIds.has(taskId)) {
                window.clearTimeout(timeoutId);
                pollingRef.current.delete(taskId);
            }
        }
    }, [state.activeSession]);

    useEffect(() => () => {
        if (chatStreamRef.current) {
            chatStreamRef.current.close();
            chatStreamRef.current = null;
        }
        for (const timeoutId of pollingRef.current.values()) {
            window.clearTimeout(timeoutId);
        }
        pollingRef.current.clear();
    }, []);

    async function loadOrganizations(fallbackOrgId = webContext?.personalOrgId) {
        try {
            const nextOrganizations = await fetchOrganizations();
            setOrganizations(nextOrganizations);
            setCurrentOrgId((previousOrgId) => resolvePreferredOrgId(nextOrganizations, previousOrgId, fallbackOrgId));
        } catch (error) {
            dispatch({ type: "error", message: error.message || "加载组织失败" });
        }
    }

    async function refreshWebContext() {
        const context = await fetchWebContext();
        setWebContext(context);
        setCurrentOrgId(context.personalOrgId ? String(context.personalOrgId) : null);
        return context;
    }

    async function loadSessions(orgId) {
        dispatch({ type: "sessions/loading" });
        try {
            const sessions = await fetchSessions(orgId);
            dispatch({ type: "sessions/loaded", sessions });
        } catch (error) {
            dispatch({ type: "error", message: error.message || "加载会话失败" });
        }
    }

    async function loadSessionById(orgId, sessionId) {
        dispatch({ type: "session/loading", sessionId });
        setSelectedSessionId(sessionId);
        const fallbackSession = findSessionSummary(state.sessions, sessionId) || state.activeSession;
        try {
            const session = await fetchSession(orgId, sessionId, fallbackSession);
            dispatch({ type: "session/loaded", session });
        } catch (error) {
            dispatch({ type: "session/error", message: error.message || "加载会话失败" });
        }
    }

    async function openRoute(pathname, orgId) {
        const route = getChatRoute(pathname);
        if (route.type === "root") {
            if (!state.sending && !state.activeSession) {
                setSelectedSessionId(null);
            }
            return;
        }
        if (route.type === "new") {
            dispatch({ type: "session/cleared" });
            setSelectedSessionId(null);
            goToHome(true);
            return;
        }
        if (route.type === "session" && route.sessionId !== state.activeSessionId) {
            await loadSessionById(orgId, route.sessionId);
        }
    }

    function handleGoHome() {
        setSelectedSessionId(null);
        dispatch({ type: "session/cleared" });
        goToHome(true);
    }

    async function handleRenameSession(session) {
        if (!currentOrgId) {
            return;
        }
        const nextTitle = window.prompt("请输入新的会话名称", session.title || "新对话");
        if (nextTitle == null) {
            return;
        }
        const normalizedTitle = nextTitle.trim();
        if (!normalizedTitle || normalizedTitle === (session.title || "").trim()) {
            return;
        }
        try {
            const renamedSession = await renameSession(currentOrgId, session.sessionId, normalizedTitle);
            dispatch({ type: "session/renamed", session: renamedSession });
        } catch (error) {
            dispatch({ type: "error", message: error.message || "重命名会话失败" });
        }
    }

    async function handleDeleteSession(session) {
        if (!currentOrgId) {
            return;
        }
        const shouldDelete = window.confirm(`确定删除对话“${session.title || "新对话"}”吗？`);
        if (!shouldDelete) {
            return;
        }
        try {
            await deleteSession(currentOrgId, session.sessionId);
            const deletingActive = state.activeSessionId === session.sessionId || selectedSessionId === session.sessionId;
            dispatch({ type: "session/deleted", sessionId: session.sessionId });
            if (deletingActive) {
                setSelectedSessionId(null);
                goToHome(true);
            }
        } catch (error) {
            dispatch({ type: "error", message: error.message || "删除会话失败" });
        }
    }

    async function handleSendMessage(content) {
        const message = content.trim();
        if (!message || !webContext || !currentOrgId) {
            return;
        }
        setBindingToken("");
        dispatch({ type: "draft/set", value: "" });

        const conversationId = selectedSessionId || state.activeSessionId || state.activeSession?.sessionId || null;
        if (conversationId) {
            setSelectedSessionId(String(conversationId));
        }

        const timestamp = Date.now();
        const pendingId = `pending-${timestamp}`;
        dispatch({
            type: "message/optimistic",
            sessionId: conversationId ? String(conversationId) : null,
            title: message,
            timestamp,
            userMessage: {
                role: "user",
                content: message,
                timestamp: new Date(timestamp).toISOString(),
                clientId: `user-${timestamp}`
            },
            pendingMessage: {
                role: "assistant",
                content: "任务已创建，正在执行中...",
                timestamp: new Date(timestamp + 1).toISOString(),
                clientId: pendingId,
                pending: true
            }
        });

        if (chatStreamRef.current) {
            chatStreamRef.current.close();
            chatStreamRef.current = null;
        }

        try {
            if (!window.EventSource) {
                throw new Error("当前浏览器不支持 SSE，请升级浏览器后重试");
            }

            const source = openAgentChatStream({
                conversationId,
                userId: String(webContext.userId),
                webUserKey: webContext.webUserKey,
                orgId: currentOrgId,
                message
            });
            chatStreamRef.current = source;

            const syncConversationId = (payload) => {
                if (!payload?.conversationId) {
                    return;
                }
                const nextSessionId = String(payload.conversationId);
                setSelectedSessionId(nextSessionId);
                dispatch({ type: "session/identified", sessionId: nextSessionId });
                if (window.location.pathname !== `/chat/${encodeURIComponent(nextSessionId)}`) {
                    replaceWithSession(nextSessionId);
                }
            };

            const finalize = async () => {
                if (chatStreamRef.current === source) {
                    chatStreamRef.current = null;
                }
                source.close();
                await loadSessions(currentOrgId);
                dispatch({ type: "sending", value: false });
            };

            const applyProgress = (payload) => {
                syncConversationId(payload);
                const taskId = payload.taskId;
                const taskStatus = payload.status || "RUNNING";
                const content = payload.message && !payload.message.startsWith("node=")
                    ? payload.message
                    : "任务执行中...";
                dispatch({
                    type: "message/replace-pending",
                    clientId: pendingId,
                    message: {
                        role: "assistant",
                        content,
                        timestamp: new Date().toISOString(),
                        clientId: pendingId,
                        taskId,
                        taskStatus,
                        pending: !isFinalTaskStatus(taskStatus)
                    },
                    keepSending: !isFinalTaskStatus(taskStatus)
                });
                if (taskId) {
                    ensureTaskPolling(taskId);
                }
            };

            const applyChunk = (payload) => {
                syncConversationId(payload);
                dispatch({
                    type: "message/append-chunk",
                    clientId: pendingId,
                    chunk: payload.content || ""
                });
            };

            const applyTerminal = async (payload, fallbackStatus, fallbackMessage) => {
                syncConversationId(payload);
                const taskId = payload.taskId;
                const taskStatus = payload.status || fallbackStatus;
                const content = payload.replyText || payload.message || fallbackMessage;
                dispatch({
                    type: "message/replace-pending",
                    clientId: pendingId,
                    message: {
                        role: "assistant",
                        content,
                        timestamp: new Date().toISOString(),
                        clientId: pendingId,
                        taskId,
                        taskStatus,
                        pending: false,
                        error: taskStatus === "FAILED" || taskStatus === "CANCELLED"
                    },
                    keepSending: false
                });
                await finalize();
            };

            source.addEventListener("progress", (event) => {
                try {
                    applyProgress(JSON.parse(event.data));
                } catch (error) {
                    dispatch({ type: "error", message: error.message || "流式进度解析失败" });
                }
            });

            source.addEventListener("chunk", (event) => {
                try {
                    applyChunk(JSON.parse(event.data));
                } catch (error) {
                    dispatch({ type: "error", message: error.message || "流式内容解析失败" });
                }
            });

            source.addEventListener("complete", async (event) => {
                try {
                    await applyTerminal(JSON.parse(event.data), "SUCCESS", "任务已完成");
                } catch (error) {
                    dispatch({ type: "error", message: error.message || "流式完成解析失败" });
                }
            });

            source.addEventListener("error", async (event) => {
                try {
                    const payload = event.data ? JSON.parse(event.data) : {};
                    await applyTerminal(payload, "FAILED", payload.message || "任务执行失败，请稍后重试");
                } catch (error) {
                    await applyTerminal({}, "FAILED", error.message || "任务执行失败，请稍后重试");
                }
            });
        } catch (error) {
            dispatch({
                type: "message/replace-pending",
                clientId: pendingId,
                message: {
                    role: "assistant",
                    content: error.message || "发送失败，请稍后重试",
                    timestamp: new Date().toISOString(),
                    clientId: `assistant-error-${Date.now()}`,
                    error: true
                },
                keepSending: false
            });
            dispatch({ type: "error", message: error.message || "发送失败，请稍后重试" });
        }
    }

    async function handleRetryTask(taskId) {
        try {
            dispatch({
                type: "message/task-update",
                taskId,
                patch: {
                    content: "任务已重新提交，正在执行中...",
                    taskStatus: "PENDING",
                    errorMessage: "",
                    error: false,
                    pending: true
                }
            });
            const retried = await retryTask(taskId);
            dispatch({
                type: "message/task-update",
                taskId,
                patch: {
                    taskStatus: retried.status,
                    content: "任务已重新提交，正在执行中...",
                    pending: true
                }
            });
            ensureTaskPolling(taskId, true);
        } catch (error) {
            dispatch({
                type: "message/task-update",
                taskId,
                patch: {
                    content: error.message || "任务重试失败，请稍后再试",
                    taskStatus: "FAILED",
                    errorMessage: error.message || "任务重试失败",
                    error: true,
                    pending: false
                }
            });
        }
    }

    function ensureTaskPolling(taskId, restart = false) {
        if (restart && pollingRef.current.has(taskId)) {
            window.clearTimeout(pollingRef.current.get(taskId));
            pollingRef.current.delete(taskId);
        }
        if (pollingRef.current.has(taskId)) {
            return;
        }

        const poll = async () => {
            try {
                const task = await fetchTaskStatus(taskId);
                dispatch({
                    type: "message/task-update",
                    taskId,
                    patch: patchFromTask(task)
                });
                if (isFinalTaskStatus(task.status)) {
                    pollingRef.current.delete(taskId);
                    if (currentOrgId) {
                        await loadSessions(currentOrgId);
                    }
                    return;
                }
                const timeoutId = window.setTimeout(poll, 1000);
                pollingRef.current.set(taskId, timeoutId);
            } catch (error) {
                dispatch({
                    type: "message/task-update",
                    taskId,
                    patch: {
                        content: error.message || "任务状态获取失败，请稍后重试",
                        taskStatus: "FAILED",
                        errorMessage: error.message || "任务状态获取失败",
                        error: true,
                        pending: false
                    }
                });
                pollingRef.current.delete(taskId);
            }
        };

        const timeoutId = window.setTimeout(poll, 1000);
        pollingRef.current.set(taskId, timeoutId);
    }

    async function handleSwitchOrganization(nextOrgId) {
        const normalizedOrgId = nextOrgId ? String(nextOrgId) : "";
        if (!normalizedOrgId || normalizedOrgId === String(currentOrgId || "")) {
            return;
        }
        setSwitchingOrg(true);
        try {
            await switchOrganization(Number(normalizedOrgId));
            setCurrentOrgId(normalizedOrgId);
            setBindingToken("");
            setSelectedSessionId(null);
            dispatch({ type: "session/cleared" });
            goToHome(true);
        } catch (error) {
            dispatch({ type: "error", message: error.message || "切换组织失败" });
        } finally {
            setSwitchingOrg(false);
        }
    }

    async function handleCreateBindToken() {
        if (!currentOrgId) {
            return;
        }
        setBindingTokenLoading(true);
        try {
            const response = await createFeishuBindToken(Number(currentOrgId));
            setBindingToken(response.token || "");
        } catch (error) {
            dispatch({ type: "error", message: error.message || "生成绑定码失败" });
        } finally {
            setBindingTokenLoading(false);
        }
    }

    async function handleAuth(action) {
        const username = (authDraft.username || "").trim();
        const password = authDraft.password || "";
        if (!username || !password) {
            dispatch({ type: "error", message: "请输入用户名和密码" });
            return;
        }
        setAuthBusy(true);
        try {
            if (action === "login") {
                await loginLocalAccount(username, password);
            } else {
                await registerLocalAccount(username, password);
            }
            await refreshAfterAuthChange();
            setAuthDraft({ username: "", password: "" });
        } catch (error) {
            dispatch({ type: "error", message: error.message || (action === "login" ? "登录失败" : "注册失败") });
        } finally {
            setAuthBusy(false);
        }
    }

    async function handleLogout() {
        setAuthBusy(true);
        try {
            await logoutLocalAccount();
            await refreshAfterAuthChange();
        } catch (error) {
            dispatch({ type: "error", message: error.message || "退出登录失败" });
        } finally {
            setAuthBusy(false);
        }
    }

    async function refreshAfterAuthChange() {
        setBindingToken("");
        setSelectedSessionId(null);
        dispatch({ type: "session/cleared" });
        goToHome(true);
        const context = await refreshWebContext();
        await loadOrganizations(context.personalOrgId);
    }

    async function handleVoiceTranscriptionResult(mode, text) {
        await handleVoiceResult(mode, text, dispatch, handleSendMessage);
    }

    const sessionTitle = useMemo(
        () => state.activeSession?.title || findSessionSummary(state.sessions, selectedSessionId)?.title || "新对话",
        [state.activeSession, state.sessions, selectedSessionId]
    );
    const headerProps = {
        userId: webContext?.userId || null,
        webUserKey: webContext?.webUserKey || "",
        authenticated: webContext?.authenticated || false,
        loginName: webContext?.loginName || "",
        organizations,
        currentOrgId,
        switchingOrg,
        binding: bindingTokenLoading,
        bindToken: bindingToken,
        authDraft,
        authBusy,
        onSwitchOrganization: handleSwitchOrganization,
        onCreateBindToken: handleCreateBindToken,
        onAuthDraftChange: (field, value) => setAuthDraft((draft) => ({ ...draft, [field]: value })),
        onLogin: () => handleAuth("login"),
        onRegister: () => handleAuth("register"),
        onLogout: handleLogout
    };
    const showConversation = Boolean(selectedSessionId || state.activeSession);
    const page = showConversation
        ? html`
            <${ConversationPage}
                title=${sessionTitle}
                session=${state.activeSession}
                sending=${state.sending}
                loading=${state.loadingSession || !contextReady}
                error=${state.error}
                draft=${state.draft}
                onDraftChange=${(value) => dispatch({ type: "draft/set", value })}
                onSend=${handleSendMessage}
                onQuickAction=${(value) => dispatch({ type: "draft/set", value })}
                onRetryTask=${handleRetryTask}
                voiceState=${state.voiceState}
                voiceMode=${state.voiceMode}
                onVoiceModeChange=${(value) => dispatch({ type: "voice/mode", value })}
                onVoiceStateChange=${(value, error = null) => dispatch({ type: "voice/state", value, error })}
                onVoiceResult=${handleVoiceTranscriptionResult}
                headerProps=${headerProps}
                debugPanelCollapsed=${debugPanelCollapsed}
                onToggleDebugPanel=${() => setDebugPanelCollapsed((value) => !value)}
            />
        `
        : html`
            <${HomePage}
                sending=${state.sending || !contextReady}
                error=${state.error}
                draft=${state.draft}
                onDraftChange=${(value) => dispatch({ type: "draft/set", value })}
                onSend=${handleSendMessage}
                onQuickAction=${(value) => dispatch({ type: "draft/set", value })}
                voiceState=${state.voiceState}
                voiceMode=${state.voiceMode}
                onVoiceModeChange=${(value) => dispatch({ type: "voice/mode", value })}
                onVoiceStateChange=${(value, error = null) => dispatch({ type: "voice/state", value, error })}
                onVoiceResult=${handleVoiceTranscriptionResult}
                headerProps=${headerProps}
                debugPanelCollapsed=${debugPanelCollapsed}
                onToggleDebugPanel=${() => setDebugPanelCollapsed((value) => !value)}
            />
        `;

    return html`
        <${ChatLayout}>
            <${Sidebar}
                sessions=${state.sessions}
                activeSessionId=${state.activeSessionId || selectedSessionId}
                collapsed=${sidebarCollapsed}
                loading=${state.loadingSessions || !contextReady}
                onCreateSession=${handleGoHome}
                onSelectSession=${(sessionId) => {
                    setSelectedSessionId(sessionId);
                    goToSession(sessionId);
                }}
                onRenameSession=${handleRenameSession}
                onDeleteSession=${handleDeleteSession}
                onToggleCollapsed=${() => setSidebarCollapsed((value) => !value)}
            />
            ${page}
        </${ChatLayout}>
    `;
}

function resolvePreferredOrgId(organizations, preferredOrgId, fallbackOrgId) {
    const normalizedPreferredOrgId = preferredOrgId ? String(preferredOrgId) : "";
    if (normalizedPreferredOrgId && organizations.some((org) => String(org.orgId) === normalizedPreferredOrgId)) {
        return normalizedPreferredOrgId;
    }
    const normalizedFallbackOrgId = fallbackOrgId ? String(fallbackOrgId) : "";
    if (normalizedFallbackOrgId && organizations.some((org) => String(org.orgId) === normalizedFallbackOrgId)) {
        return normalizedFallbackOrgId;
    }
    return organizations[0] ? String(organizations[0].orgId) : null;
}

function patchFromTask(task) {
    if (task.status === "SUCCESS") {
        return {
            content: task.result || "任务已完成",
            taskStatus: task.status,
            errorMessage: "",
            error: false,
            pending: false
        };
    }
    if (task.status === "FAILED") {
        return {
            content: `任务执行失败：${task.errorMessage || "请稍后重试"}`,
            taskStatus: task.status,
            errorMessage: task.errorMessage || "",
            error: true,
            pending: false
        };
    }
    if (task.status === "RETRYING") {
        return {
            content: `任务失败，正在第 ${task.retryCount} 次重试...`,
            taskStatus: task.status,
            errorMessage: task.errorMessage || "",
            pending: true
        };
    }
    if (task.status === "RUNNING") {
        return {
            taskStatus: task.status,
            pending: true
        };
    }
    return {
        taskStatus: task.status,
        pending: true
    };
}

function isFinalTaskStatus(status) {
    return status === "SUCCESS" || status === "FAILED" || status === "CANCELLED";
}

function identifySession(state, sessionId) {
    const normalizedId = sessionId ? String(sessionId) : null;
    if (!normalizedId) {
        return state;
    }
    const matchedSummary = findSessionSummary(state.sessions, normalizedId);
    return {
        ...state,
        activeSessionId: normalizedId,
        activeSession: state.activeSession
            ? {
                ...state.activeSession,
                sessionId: normalizedId,
                title: matchedSummary?.title || state.activeSession.title || "新对话"
            }
            : {
                sessionId: normalizedId,
                title: matchedSummary?.title || "新对话",
                messages: []
            }
    };
}

function syncSessionTitle(activeSession, sessions) {
    if (!activeSession?.sessionId) {
        return activeSession;
    }
    const matchedSummary = findSessionSummary(sessions, activeSession.sessionId);
    if (!matchedSummary?.title || matchedSummary.title === activeSession.title) {
        return activeSession;
    }
    return {
        ...activeSession,
        title: matchedSummary.title,
        updatedAt: matchedSummary.updatedAt || activeSession.updatedAt
    };
}

function findSessionSummary(sessions, sessionId) {
    if (!sessionId) {
        return null;
    }
    return sessions.find((session) => session.sessionId === String(sessionId)) || null;
}

function mergeSession(fetchedSession, existingSession) {
    if (!existingSession || existingSession.sessionId !== fetchedSession.sessionId) {
        return fetchedSession;
    }
    const fetchedMessages = fetchedSession.messages || [];
    const existingMessages = existingSession.messages || [];
    const optimisticUserMessages = existingMessages.filter((message) =>
        message.role === "user" && !hasMatchingMessage(fetchedMessages, message)
    );
    const pendingMessages = existingMessages.filter((message) =>
        message.pending || (message.taskId && message.taskStatus && message.taskStatus !== "SUCCESS")
    );
    const supplementalMessages = dedupeMessages([...optimisticUserMessages, ...pendingMessages]);
    if (!supplementalMessages.length) {
        return fetchedSession;
    }
    return {
        ...fetchedSession,
        messages: [...fetchedMessages, ...supplementalMessages]
    };
}

function hasMatchingMessage(messages, targetMessage) {
    return messages.some((message) =>
        message.role === targetMessage.role && message.content === targetMessage.content
    );
}

function dedupeMessages(messages) {
    const seen = new Set();
    return messages.filter((message) => {
        const key = message.clientId || message.serverMessageId || `${message.role}:${message.content}:${message.taskId || ""}`;
        if (seen.has(key)) {
            return false;
        }
        seen.add(key);
        return true;
    });
}

async function handleVoiceResult(mode, text, dispatch, sendMessage) {
    dispatch({ type: "voice/result", text });
    if (mode === "send") {
        await sendMessage(text);
    }
}

createRoot(document.getElementById("root")).render(html`<${App} />`);
