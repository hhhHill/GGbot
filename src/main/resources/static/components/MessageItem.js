import { html } from "../lib/html.js";

export function MessageItem({ message, onRetryTask }) {
    const role = message.role === "user" ? "user" : "assistant";
    const bubbleClass = [
        "message-bubble",
        role,
        message.pending ? "pending" : "",
        message.error ? "error" : ""
    ].filter(Boolean).join(" ");

    return html`
        <div className=${`message-row ${role}`}>
            <div className=${bubbleClass}>
                <div>${message.content}</div>
                ${message.taskStatus ? html`<div className="message-status">状态：${message.taskStatus}</div>` : null}
                ${message.errorMessage ? html`<div className="message-error-detail">${message.errorMessage}</div>` : null}
                ${message.taskStatus === "FAILED" && message.taskId
                    ? html`
                        <button
                            type="button"
                            className="retry-task-button"
                            onClick=${() => onRetryTask(message.taskId)}
                        >
                            重试
                        </button>
                    `
                    : null}
            </div>
        </div>
    `;
}
