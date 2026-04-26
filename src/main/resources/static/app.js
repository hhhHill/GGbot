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
}

function setResultPanel(data) {
    resultTaskId.textContent = data.taskId || "暂无";
    resultIntent.textContent = data.intentType || "暂无";
    resultArtifacts.textContent = data.artifactSummaries && data.artifactSummaries.length > 0
        ? data.artifactSummaries.join(" / ")
        : "暂无";
}

async function sendMessage(message) {
    appendMessage("user", message);
    appendMessage("agent", "正在处理请求，请稍候...");
    const loadingNode = chatFeed.lastElementChild;
    sendButton.disabled = true;
    sendButton.textContent = "处理中...";

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

        loadingNode.remove();
        appendMessage("agent", payload.data.replyText || "Agent 没有返回内容");
        setResultPanel(payload.data);
    } catch (error) {
        loadingNode.remove();
        appendMessage("error", error.message || "请求失败，请稍后重试", "error");
    } finally {
        sendButton.disabled = false;
        sendButton.textContent = "发送请求";
    }
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
