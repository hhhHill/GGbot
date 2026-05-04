import React, { useEffect, useRef, useState } from "react";
import { html } from "../lib/html.js";

export function SessionItem({ session, active, onSelect, onRename, onDelete }) {
    const [menuOpen, setMenuOpen] = useState(false);
    const rootRef = useRef(null);

    useEffect(() => {
        if (!menuOpen) {
            return undefined;
        }
        const handlePointerDown = (event) => {
            if (rootRef.current && !rootRef.current.contains(event.target)) {
                setMenuOpen(false);
            }
        };
        window.addEventListener("pointerdown", handlePointerDown);
        return () => window.removeEventListener("pointerdown", handlePointerDown);
    }, [menuOpen]);

    const handleRename = () => {
        setMenuOpen(false);
        onRename(session);
    };

    const handleDelete = () => {
        setMenuOpen(false);
        onDelete(session);
    };

    return html`
        <div ref=${rootRef} className=${`session-item-shell${menuOpen ? " menu-open" : ""}`}>
            <button
                type="button"
                className=${`session-item${active ? " active" : ""}`}
                onClick=${() => onSelect(session.sessionId)}
            >
                <span className="session-title">${session.title || "新对话"}</span>
            </button>
            <button
                type="button"
                className="session-menu-trigger"
                aria-label="会话操作"
                onClick=${(event) => {
                    event.stopPropagation();
                    setMenuOpen((value) => !value);
                }}
            >
                <span className="session-menu-dot"></span>
                <span className="session-menu-dot"></span>
                <span className="session-menu-dot"></span>
            </button>
            ${menuOpen ? html`
                <div className="session-menu">
                    <button type="button" className="session-menu-item" onClick=${handleRename}>重命名</button>
                    <button type="button" className="session-menu-item danger" onClick=${handleDelete}>删除对话</button>
                </div>
            ` : null}
        </div>
    `;
}
