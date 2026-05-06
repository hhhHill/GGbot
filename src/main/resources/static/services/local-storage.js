const STORAGE_KEY = "ggbot_chat_sessions";
const CLIENT_KEY_STORAGE_KEY = "ggbot_web_client_key";
const SIDEBAR_COLLAPSED_STORAGE_KEY = "ggbot_chat_sidebar_collapsed";
const DEBUG_PANEL_COLLAPSED_STORAGE_KEY = "ggbot_debug_panel_collapsed";

export function loadCachedChatState() {
    try {
        const raw = window.localStorage.getItem(STORAGE_KEY);
        if (!raw) {
            return {};
        }
        return JSON.parse(raw);
    } catch {
        return {};
    }
}

export function saveCachedChatState(state) {
    try {
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
        // ignore storage failures and continue with runtime state
    }
}

export function loadClientKey() {
    try {
        return window.localStorage.getItem(CLIENT_KEY_STORAGE_KEY) || "";
    } catch {
        return "";
    }
}

export function saveClientKey(clientKey) {
    try {
        window.localStorage.setItem(CLIENT_KEY_STORAGE_KEY, clientKey);
    } catch {
        // ignore storage failures and continue with runtime state
    }
}

export function loadSidebarCollapsedPreference() {
    try {
        return window.localStorage.getItem(SIDEBAR_COLLAPSED_STORAGE_KEY) === "true";
    } catch {
        return false;
    }
}

export function saveSidebarCollapsedPreference(collapsed) {
    try {
        window.localStorage.setItem(SIDEBAR_COLLAPSED_STORAGE_KEY, String(Boolean(collapsed)));
    } catch {
        // ignore storage failures and continue with runtime state
    }
}

export function loadDebugPanelCollapsedPreference() {
    try {
        return window.localStorage.getItem(DEBUG_PANEL_COLLAPSED_STORAGE_KEY) === "true";
    } catch {
        return false;
    }
}

export function saveDebugPanelCollapsedPreference(collapsed) {
    try {
        window.localStorage.setItem(DEBUG_PANEL_COLLAPSED_STORAGE_KEY, String(Boolean(collapsed)));
    } catch {
        // ignore storage failures and continue with runtime state
    }
}
