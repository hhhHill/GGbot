const healthDot = document.getElementById("health-dot");
const healthText = document.getElementById("health-text");
const healthDetail = document.getElementById("health-detail");
const chatFeed = document.getElementById("chat-feed");
const form = document.getElementById("chat-form");
const messageInput = document.getElementById("message-input");
const sendButton = document.getElementById("send-button");
const conversationIdInput = document.getElementById("conversation-id");
const userIdInput = document.getElementById("user-id");
const resultTaskId = document.getElementById("result-task-id");
const resultIntent = document.getElementById("result-intent");
const resultArtifacts = document.getElementById("result-artifacts");
let activeJobId = null;
let activePollToken = 0;

async function loadHealth() {
    try {
        const response = await fetch("/health");
        const data = await response.json();
        healthDot.className = "status-dot up";
        healthText.textContent = data.status || "UP";
        const llmConfigured = data.llmConfigured ? "已配置" : "未配置";
        const llmReachable = data.llmReachable ? "可连接" : "不可连接";
        healthDetail.textContent = `${data.app || "GGbot"} 已启动，LLM：${llmConfigured} / ${llmReachable}。${data.llmMessage || ""}`;
    } catch (error) {
        healthDot.className = "status-dot down";
        healthText.textContent = "不可用";
        healthDetail.textContent = "无法访问 /health，请确认 Spring Boot 服务已经启动";
    }
}

function appendMessage(role, content, extraClass = "") {
    const article = document.createElement("article");
    article.className = `message ${role} ${extraClass}`.trim();

    const roleNode = document.createElement("div");
    roleNode.className = "message-role";
    roleNode.textContent = role === "user" ? "You" : role === "error" ? "Error" : "Agent";

    const bodyNode = document.createElement("div");
    bodyNode.className = "message-body";
    bodyNode.textContent = content;

    article.append(roleNode, bodyNode);
    chatFeed.appendChild(article);
    chatFeed.scrollTop = chatFeed.scrollHeight;
    return { article, roleNode, bodyNode };
}

function setResultPanel(data) {
    resultTaskId.textContent = data.taskId || data.jobId || "暂无";
    resultIntent.textContent = data.intentType || data.status || "暂无";
    resultArtifacts.textContent = data.artifactSummaries && data.artifactSummaries.length > 0
        ? data.artifactSummaries.join(" / ")
        : "暂无";
}

function setBusyState(isBusy, label = "发送请求") {
    sendButton.disabled = isBusy;
    messageInput.disabled = isBusy;
    conversationIdInput.disabled = isBusy;
    userIdInput.disabled = isBusy;
    sendButton.querySelector(".button-label").textContent = label;
    sendButton.querySelector(".spinner").classList.toggle("hidden", !isBusy);
}

function clearMessageActions(article) {
    article.querySelector(".message-actions")?.remove();
}

function setMessageState(messageNode, role, extraClass, content) {
    messageNode.article.className = `message ${role} ${extraClass}`.trim();
    messageNode.roleNode.textContent = role === "user" ? "You" : role === "error" ? "Error" : "Agent";
    messageNode.bodyNode.textContent = content;
    clearMessageActions(messageNode.article);
    chatFeed.scrollTop = chatFeed.scrollHeight;
}

function addRetryButton(messageNode, jobId) {
    clearMessageActions(messageNode.article);
    const actions = document.createElement("div");
    actions.className = "message-actions";

    const retryButton = document.createElement("button");
    retryButton.type = "button";
    retryButton.className = "ghost-button retry-button";
    retryButton.textContent = "重传请求";
    retryButton.addEventListener("click", () => {
        retryJob(jobId, messageNode);
    });

    actions.appendChild(retryButton);
    messageNode.article.appendChild(actions);
}

async function sendMessage(message) {
    appendMessage("user", message);
    const messageNode = appendMessage("agent", "正在生成中...", "pending");
    setBusyState(true, "处理中...");

    try {
        const response = await fetch("/api/agent/chat", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                conversationId: conversationIdInput.value || "web-mvp-session",
                userId: userIdInput.value || "demo-user",
                message
            })
        });
        const payload = await response.json();

        if (!response.ok || !payload.success) {
            throw new Error(payload.message || "请求失败");
        }

        if (payload.data.accepted) {
            activeJobId = payload.data.jobId;
            setMessageState(messageNode, "agent", "pending", "任务已提交，正在处理中...");
            setResultPanel(payload.data);
            await pollJob(activeJobId, messageNode);
            return;
        }

        setMessageState(messageNode, "agent", "", payload.data.replyText || "Agent 没有返回内容");
        setResultPanel(payload.data);
        setBusyState(false);
    } catch (error) {
        setMessageState(messageNode, "error", "error", error.message || "请求失败，请稍后重试");
        setBusyState(false);
    }
}

async function retryJob(jobId, messageNode) {
    setBusyState(true, "重试中...");
    setMessageState(messageNode, "agent", "pending", "正在重新提交请求...");

    try {
        const response = await fetch(`/api/agent/jobs/${jobId}/retry`, {
            method: "POST"
        });
        const payload = await response.json();

        if (!response.ok || !payload.success) {
            throw new Error(payload.message || "重试失败");
        }

        activeJobId = payload.data.jobId;
        setResultPanel(payload.data);
        setMessageState(messageNode, "agent", "pending", "任务已重新提交，正在处理中...");
        await pollJob(activeJobId, messageNode);
    } catch (error) {
        setMessageState(messageNode, "error", "error", error.message || "重试失败，请稍后再试");
        setBusyState(false);
    }
}

async function pollJob(jobId, messageNode) {
    const pollToken = ++activePollToken;

    while (activeJobId === jobId && pollToken === activePollToken) {
        await sleep(1200);

        try {
            const response = await fetch(`/api/agent/jobs/${jobId}`);
            const payload = await response.json();

            if (!response.ok || !payload.success) {
                throw new Error(payload.message || "获取任务状态失败");
            }

            const data = payload.data;
            if (data.status === "SUCCEEDED") {
                activeJobId = null;
                setMessageState(messageNode, "agent", "", data.replyText || "任务已完成");
                setResultPanel(data);
                setBusyState(false);
                return;
            }

            if (data.status === "FAILED" || data.status === "TIMEOUT") {
                activeJobId = null;
                setMessageState(messageNode, "error", "error", data.fallbackReply || "请求处理失败，请稍后重试");
                setResultPanel(data);
                if (data.canRetry) {
                    addRetryButton(messageNode, jobId);
                }
                setBusyState(false);
                return;
            }

            setMessageState(messageNode, "agent", "pending", data.progressMessage || "任务处理中...");
            setResultPanel(data);
        } catch (error) {
            setMessageState(messageNode, "agent", "pending", "任务状态获取失败，正在继续重试...");
        }
    }
}

function sleep(ms) {
    return new Promise((resolve) => window.setTimeout(resolve, ms));
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const message = messageInput.value.trim();
    if (!message) {
        appendMessage("error", "请输入内容后再发送", "error");
        return;
    }
    messageInput.value = "";
    await sendMessage(message);
});

document.querySelectorAll(".example-button").forEach((button) => {
    button.addEventListener("click", () => {
        messageInput.value = button.dataset.example || "";
        messageInput.focus();
    });
});

loadHealth();
