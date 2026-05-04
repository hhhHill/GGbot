import { html } from "../lib/html.js";
import { ChatHeader } from "./ChatHeader.js";
import { MessageList } from "./MessageList.js";
import { InputBox } from "./InputBox.js";
import { QuickActions } from "./QuickActions.js";

export function ChatPage({ viewMode, title, session, sending, loading, error, draft, onDraftChange, onSend, onQuickAction, onRetryTask }) {
    const messages = session?.messages || [];
    const isHomeView = viewMode === "home";
    const isEmpty = !loading && messages.length === 0;
    const emptyContent = isHomeView
        ? html`
            <section className="empty-state-shell">
                <h2 className="empty-title">开始一个新的对话</h2>
                <p className="empty-subtitle">你可以直接输入问题，或使用下方快捷能力开始生成。</p>
                <${QuickActions} tone="hero" onPick=${onQuickAction} />
            </section>
        `
        : html`
            <section className="empty-state-shell session-shell">
                <h2 className="empty-title">对话已创建</h2>
                <p className="empty-subtitle">当前正在进入会话页面，消息会显示在这里。</p>
            </section>
        `;

    return html`
        <section className=${`chat-page ${isHomeView ? "home-view" : "session-view"}`}>
            <${ChatHeader} title=${title} />
            <${MessageList}
                messages=${messages}
                loading=${loading}
                onRetryTask=${onRetryTask}
                emptyContent=${emptyContent}
            />
            <div className="composer-wrap">
                <${InputBox}
                    sending=${sending}
                    error=${error}
                    value=${draft}
                    onChange=${onDraftChange}
                    onSend=${onSend}
                    quickActions=${!isEmpty ? html`<${QuickActions} tone="inline" onPick=${onQuickAction} />` : null}
                />
            </div>
        </section>
    `;
}
