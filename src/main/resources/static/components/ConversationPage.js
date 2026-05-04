import { html } from "../lib/html.js";
import { ChatHeader } from "./ChatHeader.js";
import { MessageList } from "./MessageList.js";
import { InputBox } from "./InputBox.js";
import { QuickActions } from "./QuickActions.js";

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
    onRetryTask
}) {
    const messages = session?.messages || [];
    const isEmpty = !loading && messages.length === 0;

    return html`
        <section className="chat-page session-view">
            <${ChatHeader} title=${title} />
            <${MessageList}
                messages=${messages}
                loading=${loading}
                onRetryTask=${onRetryTask}
                emptyContent=${html`
                    <section className="empty-state-shell session-shell">
                        <h2 className="empty-title">对话已创建</h2>
                        <p className="empty-subtitle">当前会话已打开，等待消息返回。</p>
                    </section>
                `}
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
