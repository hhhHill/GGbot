import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";

const appJsPath = path.resolve("src/main/resources/static/app.js");
const appJsSource = fs.readFileSync(appJsPath, "utf8");
const mergeSessionSource = extractFunctionSource(appJsSource, "mergeSession");
const hasMatchingMessageSource = extractFunctionSource(appJsSource, "hasMatchingMessage");
const dedupeMessagesSource = extractFunctionSource(appJsSource, "dedupeMessages");
const reducerSource = extractFunctionSource(appJsSource, "reducer");
const mergeSession = vm.runInNewContext(`
${mergeSessionSource}
${hasMatchingMessageSource}
${dedupeMessagesSource}
mergeSession;
`);
const reducer = vm.runInNewContext(`
${mergeSessionSource}
${hasMatchingMessageSource}
${dedupeMessagesSource}
${reducerSource}
reducer;
`);

const fetchedSession = {
    sessionId: "session-1",
    title: "新对话",
    messages: []
};
const existingSession = {
    sessionId: "session-1",
    title: "新对话",
    messages: [
        { role: "user", content: "你好", clientId: "user-1" },
        { role: "assistant", content: "任务已创建，正在执行中...", clientId: "pending-1", pending: true }
    ]
};

const merged = mergeSession(fetchedSession, existingSession);

assert.equal(merged.messages.length, 2);
assert.deepEqual(
    Array.from(merged.messages, (message) => message.content),
    ["你好", "任务已创建，正在执行中..."]
);

const partiallyFetchedSession = {
    sessionId: "session-1",
    title: "新对话",
    messages: [
        { role: "user", content: "你好" }
    ]
};

const partiallyMerged = mergeSession(partiallyFetchedSession, existingSession);

assert.equal(partiallyMerged.messages.length, 2);
assert.deepEqual(
    Array.from(partiallyMerged.messages, (message) => message.content),
    ["你好", "任务已创建，正在执行中..."]
);

const optimisticState = reducer(
    {
        activeSessionId: null,
        activeSession: null,
        sending: false,
        error: ""
    },
    {
        type: "message/optimistic",
        sessionId: "session-1",
        title: "第一条消息",
        userMessage: { role: "user", content: "第一条消息", clientId: "user-2" },
        pendingMessage: { role: "assistant", content: "任务已创建，正在执行中...", clientId: "pending-2", pending: true }
    }
);

assert.equal(optimisticState.activeSessionId, "session-1");
assert.equal(optimisticState.activeSession.sessionId, "session-1");
assert.equal(optimisticState.activeSession.title, "第一条消息");

console.log("app-merge-session test passed");

function extractFunctionSource(source, functionName) {
    const marker = `function ${functionName}`;
    const startIndex = source.indexOf(marker);
    if (startIndex === -1) {
        throw new Error(`Function not found: ${functionName}`);
    }
    const bodyStartIndex = source.indexOf("{", startIndex);
    let depth = 0;
    for (let index = bodyStartIndex; index < source.length; index += 1) {
        const char = source[index];
        if (char === "{") {
            depth += 1;
        } else if (char === "}") {
            depth -= 1;
            if (depth === 0) {
                return source.slice(startIndex, index + 1);
            }
        }
    }
    throw new Error(`Failed to parse function: ${functionName}`);
}
