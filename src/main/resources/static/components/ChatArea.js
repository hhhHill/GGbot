import { html } from "../lib/html.js";
import { ChatHeader } from "./ChatHeader.js";
import { MessageList } from "./MessageList.js";
import { InputBox } from "./InputBox.js";
import { QuickActions } from "./QuickActions.js";

export function ChatArea({
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
    emptyContent,
    quickActionsTone = null,
    voiceState = "idle",
    voiceMode = "fill",
    onVoiceModeChange,
    onVoiceStateChange,
    onVoiceResult
}) {
    const messages = session?.messages || [];
    const isEmpty = !loading && messages.length === 0;

    return html`
        <section className="chat-area">
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
                    quickActions=${quickActionsTone && !isEmpty ? html`<${QuickActions} tone=${quickActionsTone} onPick=${onQuickAction} />` : null}
                    voiceState=${voiceState}
                    voiceMode=${voiceMode}
                    onVoiceModeChange=${onVoiceModeChange}
                    onVoiceStateChange=${onVoiceStateChange}
                    onVoiceResult=${onVoiceResult}
                />
            </div>
        </section>
    `;
}
