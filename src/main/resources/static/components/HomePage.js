import { html } from "../lib/html.js";
import { ChatHeader } from "./ChatHeader.js";
import { MessageList } from "./MessageList.js";
import { InputBox } from "./InputBox.js";
import { QuickActions } from "./QuickActions.js";

export function HomePage({ sending, error, draft, onDraftChange, onSend, onQuickAction }) {
    return html`
        <section className="chat-page home-view">
            <${ChatHeader} title="新对话" />
            <${MessageList}
                messages=${[]}
                loading=${false}
                onRetryTask=${() => {}}
                emptyContent=${html`
                    <section className="empty-state-shell">
                        <h2 className="empty-title">开始一个新的对话</h2>
                        <p className="empty-subtitle">你可以直接输入问题，或使用下方快捷能力开始生成。</p>
                        <${QuickActions} tone="hero" onPick=${onQuickAction} />
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
                    quickActions=${null}
                />
            </div>
        </section>
    `;
}
