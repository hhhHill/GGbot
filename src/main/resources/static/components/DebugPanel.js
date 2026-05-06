import { html } from "../lib/html.js";

export function DebugPanel({
    userId,
    webUserKey,
    authenticated = false,
    loginName = "",
    organizations = [],
    currentOrgId,
    switchingOrg = false,
    binding = false,
    bindToken = "",
    authDraft = { username: "", password: "" },
    authBusy = false,
    onSwitchOrganization,
    onCreateBindToken,
    onAuthDraftChange,
    onLogin,
    onRegister,
    onLogout,
    collapsed = false,
    onToggleCollapsed
}) {
    const currentOrganization = organizations.find((org) => String(org.orgId) === String(currentOrgId)) || null;
    const workspaceName = currentOrganization ? currentOrganization.name || `组织 ${currentOrganization.orgId}` : "等待 Workspace 上下文加载";
    const bindingStatus = bindToken ? "已生成绑定码" : authenticated ? "未生成绑定码" : "未登录";

    return html`
        <section className="debug-panel">
            <div className="debug-panel-header">
                <div>
                    <p className="debug-panel-eyebrow">Debug Panel</p>
                    <h2 className="debug-panel-title">调试信息面板</h2>
                </div>
                <div className="debug-panel-header-actions">
                    <p className="debug-panel-subtitle">右侧抽屉展示调试信息，避免遮挡主对话内容。</p>
                    <button className="chat-secondary-button debug-toggle-button" type="button" onClick=${() => onToggleCollapsed?.()}>
                        ${collapsed ? "展开 Debug Panel" : "关闭 Debug Panel"}
                    </button>
                </div>
            </div>
            ${collapsed
                ? html`
                    <div className="debug-panel-summary">
                        <div className="debug-summary-item">
                            <span className="debug-field-label">账号状态</span>
                            <div className="debug-static-value">${authenticated ? `已登录：${loginName || "本地账号"}` : "未登录"}</div>
                        </div>
                        <div className="debug-summary-item">
                            <span className="debug-field-label">当前 Workspace</span>
                            <div className="debug-static-value">${workspaceName}</div>
                        </div>
                        <div className="debug-summary-item">
                            <span className="debug-field-label">绑定码状态</span>
                            <div className="debug-static-value">${bindingStatus}</div>
                        </div>
                    </div>
                `
                : html`<div className="debug-panel-grid">
                <section className="debug-card">
                    <div className="debug-card-header">
                        <h3 className="debug-card-title">账号信息</h3>
                        <span className=${`debug-status-chip ${authenticated ? "success" : "muted"}`}>
                            ${authenticated ? "已登录" : "未登录"}
                        </span>
                    </div>
                    <div className="debug-card-body">
                        <div className="debug-meta-grid">
                            <div className="debug-meta-item">
                                <span className="debug-field-label">User ID</span>
                                <code className="debug-inline-code">${userId || "-"}</code>
                            </div>
                            <div className="debug-meta-item">
                                <span className="debug-field-label">Web User Key</span>
                                <code className="debug-inline-code">${webUserKey || "-"}</code>
                            </div>
                        </div>
                        ${authenticated
                            ? html`
                                <div className="debug-auth-logged">
                                    <div className="debug-field">
                                        <span className="debug-field-label">当前账号</span>
                                        <div className="debug-static-value">${loginName || "本地账号"}</div>
                                    </div>
                                    <button className="chat-secondary-button" type="button" onClick=${() => onLogout?.()}>
                                        退出登录
                                    </button>
                                </div>
                            `
                            : html`
                                <div className="debug-form-grid auth">
                                    <label className="debug-field">
                                        <span className="debug-field-label">用户名</span>
                                        <input
                                            className="chat-auth-input"
                                            type="text"
                                            value=${authDraft.username || ""}
                                            placeholder="请输入用户名"
                                            disabled=${authBusy}
                                            onInput=${(event) => onAuthDraftChange?.("username", event.target.value)}
                                        />
                                    </label>
                                    <label className="debug-field">
                                        <span className="debug-field-label">密码</span>
                                        <input
                                            className="chat-auth-input"
                                            type="password"
                                            value=${authDraft.password || ""}
                                            placeholder="请输入密码"
                                            disabled=${authBusy}
                                            onInput=${(event) => onAuthDraftChange?.("password", event.target.value)}
                                        />
                                    </label>
                                </div>
                                <div className="debug-action-row">
                                    <button className="chat-secondary-button" type="button" disabled=${authBusy} onClick=${() => onLogin?.()}>
                                        ${authBusy ? "处理中..." : "登录"}
                                    </button>
                                    <button className="chat-tool-button" type="button" disabled=${authBusy} onClick=${() => onRegister?.()}>
                                        ${authBusy ? "处理中..." : "注册"}
                                    </button>
                                </div>
                            `}
                    </div>
                </section>

                <section className="debug-card">
                    <div className="debug-card-header">
                        <h3 className="debug-card-title">Workspace 选择</h3>
                        <span className="debug-status-chip muted">Workspace</span>
                    </div>
                    <div className="debug-card-body">
                        <label className="debug-field">
                            <span className="debug-field-label">当前 Workspace</span>
                            <select
                                className="chat-org-switch"
                                value=${currentOrgId || ""}
                                disabled=${switchingOrg || organizations.length === 0}
                                onChange=${(event) => onSwitchOrganization?.(event.target.value)}
                            >
                                ${organizations.map((org) => html`
                                    <option value=${String(org.orgId)}>
                                        ${org.name || `组织 ${org.orgId}`}
                                    </option>
                                `)}
                            </select>
                        </label>
                        <div className="debug-meta-item">
                            <span className="debug-field-label">当前选择</span>
                            <div className="debug-static-value">${workspaceName}</div>
                        </div>
                    </div>
                </section>

                <section className="debug-card">
                    <div className="debug-card-header">
                        <h3 className="debug-card-title">API Key 管理</h3>
                        <span className="debug-status-chip muted">飞书绑定</span>
                    </div>
                    <div className="debug-card-body">
                        <div className="debug-action-stack">
                            <button
                                className="chat-tool-button"
                                type="button"
                                disabled=${binding || !currentOrgId || !authenticated}
                                onClick=${() => onCreateBindToken?.()}
                            >
                                ${binding ? "生成中..." : "生成飞书绑定码"}
                            </button>
                            <div className="chat-bind-token">
                                ${bindToken
                                    ? html`<span>绑定码：<code>${bindToken}</code></span>`
                                    : html`<span>${!authenticated
                                        ? "请先登录，再为当前 Workspace 生成绑定码。"
                                        : currentOrganization
                                            ? "切换 Workspace 后可立即生成当前绑定码。"
                                            : "等待 Workspace 上下文加载。"}</span>`}
                            </div>
                        </div>
                    </div>
                </section>
            </div>`}
        </section>
    `;
}
