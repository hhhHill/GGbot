import React, { useEffect, useRef } from "react";
import { html } from "../lib/html.js";
import { MessageItem } from "./MessageItem.js";

export function MessageList({ messages, loading, emptyContent, onRetryTask }) {
    const scrollRef = useRef(null);

    useEffect(() => {
        if (!scrollRef.current) {
            return;
        }
        scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }, [messages.length, loading]);

    return html`
        <div className="message-scroll" ref=${scrollRef}>
            <div className="message-list">
                ${loading ? html`<div className="empty-state">加载会话中...</div>` : null}
                ${!loading && messages.length === 0 ? html`<div className="empty-state">${emptyContent}</div>` : null}
                ${messages.map((message, index) => html`
                    <${MessageItem}
                        key=${message.clientId || `${message.timestamp}-${index}`}
                        message=${message}
                        onRetryTask=${onRetryTask}
                    />
                `)}
            </div>
        </div>
    `;
}
