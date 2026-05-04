import { html } from "../lib/html.js";
import { SessionItem } from "./SessionItem.js";

export function Sidebar({
    sessions,
    activeSessionId,
    collapsed,
    loading,
    onCreateSession,
    onSelectSession,
    onRenameSession,
    onDeleteSession,
    onToggleCollapsed
}) {
    return html`
        <aside className=${`sidebar ${collapsed ? "collapsed" : ""}`}>
            <div className="sidebar-header">
                ${collapsed ? html`
                    <button
                        className="sidebar-collapsed-toggle"
                        type="button"
                        onClick=${onToggleCollapsed}
                        aria-label="展开会话记录栏"
                        title="展开会话记录栏"
                    >
                        <span className="collapsed-brand-glyph">G</span>
                        <span className="collapsed-toggle-text">展开</span>
                    </button>
                ` : html`
                    <div className="sidebar-header-top">
                        <div className="sidebar-brand">
                            <span className="brand-mark">GGbot Chat</span>
                            <p className="brand-caption">AI 工作台</p>
                        </div>
                        <button
                            className="sidebar-toggle-button"
                            type="button"
                            onClick=${onToggleCollapsed}
                            aria-label="收起会话记录栏"
                            title="收起会话记录栏"
                        >
                            收起
                        </button>
                    </div>
                    <button className="primary-button sidebar-create-button" type="button" onClick=${onCreateSession}>新建会话</button>
                `}
            </div>
            <div className="session-list">
                ${loading && sessions.length === 0 ? html`<div className="sidebar-loading">会话加载中...</div>` : null}
                ${!loading && sessions.length === 0 ? html`<div className="sidebar-empty">暂无历史会话</div>` : null}
                ${sessions.map((session) => html`
                    <${SessionItem}
                        key=${session.sessionId}
                        session=${session}
                        active=${session.sessionId === activeSessionId}
                        onSelect=${onSelectSession}
                        onRename=${onRenameSession}
                        onDelete=${onDeleteSession}
                    />
                `)}
            </div>
        </aside>
    `;
}
