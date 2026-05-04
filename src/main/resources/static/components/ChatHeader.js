import { html } from "../lib/html.js";

export function ChatHeader({ title }) {
    return html`
        <header className="chat-header">
            <div className="chat-header-inner">
                <h1 className="chat-title">${title || "新对话"}</h1>
            </div>
        </header>
    `;
}
