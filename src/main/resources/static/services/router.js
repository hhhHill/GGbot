export function getChatRoute(pathname) {
    if (!pathname || pathname === "/") {
        return { type: "root" };
    }
    if (pathname === "/chat/new") {
        return { type: "new" };
    }
    const match = pathname.match(/^\/chat\/([^/]+)$/);
    if (match) {
        return { type: "session", sessionId: decodeURIComponent(match[1]) };
    }
    return { type: "root" };
}

export function goToNewChat(replace = false) {
    navigate("/", replace);
}

export function goToHome(replace = false) {
    navigate("/", replace);
}

export function goToSession(sessionId, replace = false) {
    navigate(`/chat/${encodeURIComponent(sessionId)}`, replace);
}

export function replaceWithSession(sessionId) {
    goToSession(sessionId, true);
}

export function subscribeRouteChange(handler) {
    const listener = () => handler(window.location.pathname);
    window.addEventListener("popstate", listener);
    return () => window.removeEventListener("popstate", listener);
}

function navigate(pathname, replace) {
    const method = replace ? "replaceState" : "pushState";
    window.history[method]({}, "", pathname);
    window.dispatchEvent(new PopStateEvent("popstate"));
}
