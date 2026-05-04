import { html } from "../lib/html.js";

export function ChatLayout({ children }) {
    return html`<main className="app-shell">${children}</main>`;
}
