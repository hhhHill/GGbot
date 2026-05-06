import { html } from "../lib/html.js";
import { DebugPanel } from "./DebugPanel.js";
import { ChatArea } from "./ChatArea.js";

export function ConversationPage({
    title,
    session,
    sending,
    loading,
    error,
    draft,
    onDraftChange,
    onSend,
    onQuickAction,
    onRetryTask,
    voiceState,
    voiceMode,
    onVoiceModeChange,
    onVoiceStateChange,
    onVoiceResult,
    headerProps = {},
    debugPanelCollapsed = false,
    onToggleDebugPanel
}) {
    return html`
        <section className="chat-page session-view">
            <button
                className="chat-secondary-button debug-drawer-toggle"
                type="button"
                aria-expanded=${String(!debugPanelCollapsed)}
                onClick=${onToggleDebugPanel}
            >
                ${debugPanelCollapsed ? "打开 Debug" : "关闭 Debug"}
            </button>
            <${ChatArea}
                title=${title}
                session=${session}
                loading=${loading}
                sending=${sending}
                error=${error}
                draft=${draft}
                onDraftChange=${onDraftChange}
                onSend=${onSend}
                onQuickAction=${onQuickAction}
                onRetryTask=${onRetryTask}
                voiceState=${voiceState}
                voiceMode=${voiceMode}
                onVoiceModeChange=${onVoiceModeChange}
                onVoiceStateChange=${onVoiceStateChange}
                onVoiceResult=${onVoiceResult}
                quickActionsTone="inline"
                emptyContent=${html`
                    <section className="empty-state-shell session-shell">
                        <h2 className="empty-title">对话已创建</h2>
                        <p className="empty-subtitle">当前会话已打开，等待消息返回。</p>
                    </section>
                `}
            />
            ${debugPanelCollapsed ? null : html`
                <button className="debug-drawer-backdrop" type="button" aria-label="关闭 Debug Panel" onClick=${onToggleDebugPanel}></button>
            `}
            <aside className=${`debug-drawer-shell${debugPanelCollapsed ? "" : " open"}`} aria-hidden=${String(debugPanelCollapsed)}>
                <${DebugPanel}
                    userId=${headerProps.userId}
                    webUserKey=${headerProps.webUserKey}
                    authenticated=${headerProps.authenticated}
                    loginName=${headerProps.loginName}
                    organizations=${headerProps.organizations}
                    currentOrgId=${headerProps.currentOrgId}
                    switchingOrg=${headerProps.switchingOrg}
                    binding=${headerProps.binding}
                    bindToken=${headerProps.bindToken}
                    authDraft=${headerProps.authDraft}
                    authBusy=${headerProps.authBusy}
                    onSwitchOrganization=${headerProps.onSwitchOrganization}
                    onCreateBindToken=${headerProps.onCreateBindToken}
                    onAuthDraftChange=${headerProps.onAuthDraftChange}
                    onLogin=${headerProps.onLogin}
                    onRegister=${headerProps.onRegister}
                    onLogout=${headerProps.onLogout}
                    collapsed=${debugPanelCollapsed}
                    onToggleCollapsed=${onToggleDebugPanel}
                />
            </aside>
        </section>
    `;
}
