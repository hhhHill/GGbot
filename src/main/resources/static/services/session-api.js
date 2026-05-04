import { loadClientKey, saveClientKey } from "./local-storage.js";

function createClientKey() {
    if (window.crypto?.randomUUID) {
        return `web-${window.crypto.randomUUID()}`;
    }
    return `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function getOrCreateClientKey() {
    const existingKey = loadClientKey();
    if (existingKey) {
        return existingKey;
    }
    const nextKey = createClientKey();
    saveClientKey(nextKey);
    return nextKey;
}

async function request(url, options = {}) {
    const response = await fetch(url, options);
    const payload = await response.json();
    if (!response.ok || !payload.success) {
        throw new Error(payload.message || "请求失败");
    }
    return payload.data;
}

function mapConversationSummary(summary) {
    return {
        sessionId: String(summary.conversationId),
        title: summary.title || "新对话",
        createdAt: summary.lastMessageAt || null,
        updatedAt: summary.lastMessageAt || null,
        source: summary.source || "web",
        status: summary.status || null
    };
}

function mapConversationMessage(message) {
    return {
        role: message.role ? message.role.toLowerCase() : "assistant",
        content: message.content || "",
        timestamp: message.createdAt || null,
        serverMessageId: message.messageId ? String(message.messageId) : null
    };
}

export async function fetchWebContext() {
    const clientKey = getOrCreateClientKey();
    return request(`/api/web/context?clientKey=${encodeURIComponent(clientKey)}`);
}

export async function fetchSessions(orgId) {
    const conversations = await request(`/api/conversations?orgId=${encodeURIComponent(orgId)}`);
    return conversations.map(mapConversationSummary);
}

export async function fetchSession(orgId, conversationId, fallbackSession = null) {
    const messages = await request(
        `/api/conversations/${encodeURIComponent(conversationId)}/messages?orgId=${encodeURIComponent(orgId)}`
    );
    return {
        sessionId: String(conversationId),
        title: fallbackSession?.title || "新对话",
        createdAt: fallbackSession?.createdAt || null,
        updatedAt: fallbackSession?.updatedAt || null,
        messages: messages.map(mapConversationMessage)
    };
}

export async function renameSession(orgId, conversationId, title) {
    const summary = await request(`/api/conversations/${encodeURIComponent(conversationId)}/title?orgId=${encodeURIComponent(orgId)}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title })
    });
    return mapConversationSummary(summary);
}

export async function deleteSession(orgId, conversationId) {
    return request(`/api/conversations/${encodeURIComponent(conversationId)}?orgId=${encodeURIComponent(orgId)}`, {
        method: "DELETE",
        headers: { "Content-Type": "application/json" }
    });
}

export async function sendChatMessage(body) {
    return request("/api/chat/send", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
}

export function openAgentChatStream(body) {
    const url = new URL("/api/agent/chat/stream", window.location.origin);
    Object.entries(body).forEach(([key, value]) => {
        if (value !== undefined && value !== null && value !== "") {
            url.searchParams.set(key, String(value));
        }
    });
    return new EventSource(url.toString());
}

export async function fetchTaskStatus(taskId) {
    return request(`/api/tasks/${encodeURIComponent(taskId)}`);
}

export async function retryTask(taskId) {
    return request(`/api/tasks/${encodeURIComponent(taskId)}/retry`, {
        method: "POST",
        headers: { "Content-Type": "application/json" }
    });
}
