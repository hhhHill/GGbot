import React, { useEffect, useRef } from "react";
import { html } from "../lib/html.js";

export function InputBox({ sending, error, value, onChange, onSend, quickActions }) {
    const textareaRef = useRef(null);

    useEffect(() => {
        if (!textareaRef.current) {
            return;
        }
        textareaRef.current.style.height = "0px";
        textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 160)}px`;
    }, [value]);

    async function handleSubmit(event) {
        event.preventDefault();
        if (!value.trim() || sending) {
            return;
        }
        await onSend(value);
    }

    function handleKeyDown(event) {
        if (event.key === "Enter" && !event.shiftKey) {
            event.preventDefault();
            void handleSubmit(event);
        }
    }

    return html`
        <div className="composer-inner">
            ${quickActions ? html`<div className="composer-quick-actions">${quickActions}</div>` : null}
            <form className="composer-panel" onSubmit=${handleSubmit}>
                <div className="composer-shell">
                    <textarea
                        ref=${textareaRef}
                        className="composer-input"
                        placeholder="给 GGbot 发送消息"
                        value=${value}
                        onInput=${(event) => onChange(event.target.value)}
                        onKeyDown=${handleKeyDown}
                    />
                    <div className="composer-footer">
                        <div>
                            <div className="composer-hint">Enter 发送，Shift + Enter 换行</div>
                            ${error ? html`<div className="composer-error">${error}</div>` : null}
                        </div>
                        <button className="primary-button send-button" type="submit" disabled=${sending || !value.trim()}>
                            ${sending ? "发送中..." : "发送"}
                        </button>
                    </div>
                </div>
            </form>
        </div>
    `;
}
