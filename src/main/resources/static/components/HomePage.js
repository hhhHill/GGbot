import { html } from "../lib/html.js";
import { QuickActions } from "./QuickActions.js";
import { DebugPanel } from "./DebugPanel.js";
import { ChatArea } from "./ChatArea.js";

export function HomePage({
    sending,
    error,
    draft,
    onDraftChange,
    onSend,
    onQuickAction,
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
        <section className="chat-page home-view">
            <button
                className="chat-secondary-button debug-drawer-toggle"
                type="button"
                aria-expanded=${String(!debugPanelCollapsed)}
                onClick=${onToggleDebugPanel}
            >
                ${debugPanelCollapsed ? "打开 Debug" : "关闭 Debug"}
            </button>
            <${ChatArea}
                title="新对话"
                session=${null}
                loading=${false}
                sending=${sending}
                error=${error}
                draft=${draft}
                onDraftChange=${onDraftChange}
                onSend=${onSend}
                onQuickAction=${onQuickAction}
                onRetryTask=${() => {}}
                voiceState=${voiceState}
                voiceMode=${voiceMode}
                onVoiceModeChange=${onVoiceModeChange}
                onVoiceStateChange=${onVoiceStateChange}
                onVoiceResult=${onVoiceResult}
                quickActionsTone=${null}
                emptyContent=${html`
                    <section className="empty-state-shell">
                        <h2 className="empty-title">开始一个新的对话</h2>
                        <p className="empty-subtitle">你可以直接输入问题，或使用下方快捷能力开始生成。</p>
                        <${QuickActions} tone="hero" onPick=${onQuickAction} />
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
