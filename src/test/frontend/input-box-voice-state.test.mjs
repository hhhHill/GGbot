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
const handleVoiceResultSource = extractFunctionSource(appJsSource, "handleVoiceResult");

const reducer = vm.runInNewContext(`
${mergeSessionSource}
${hasMatchingMessageSource}
${dedupeMessagesSource}
${reducerSource}
reducer;
`);

const handleVoiceResult = vm.runInNewContext(`
${handleVoiceResultSource}
handleVoiceResult;
`);

const state = reducer(
    {
        draft: "",
        voiceMode: "fill",
        voiceState: "transcribing",
        error: ""
    },
    {
        type: "voice/result",
        text: "帮我输出项目方案"
    }
);

assert.equal(state.draft, "帮我输出项目方案");
assert.equal(state.voiceState, "idle");
console.log("voice draft fill reducer test passed");

const dispatched = [];
let sentMessage = "";

await handleVoiceResult("send", "直接发出去", (action) => {
    dispatched.push(action);
}, async (message) => {
    sentMessage = message;
});

assert.equal(dispatched.length, 1);
assert.equal(dispatched[0].type, "voice/result");
assert.equal(dispatched[0].text, "直接发出去");
assert.equal(sentMessage, "直接发出去");
console.log("voice direct send handler test passed");

function extractFunctionSource(source, functionName) {
    const markers = [`async function ${functionName}`, `function ${functionName}`];
    const startIndex = markers
        .map((marker) => source.indexOf(marker))
        .find((index) => index !== -1);
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
