import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import vm from "node:vm";

const servicePath = path.resolve("src/main/resources/static/services/voice-input.js");
const serviceSource = fs.readFileSync(servicePath, "utf8");

const context = {
    Blob,
    FormData,
    fetch: async (url, options) => {
        context.calls.push({ url, options });
        return {
            ok: true,
            async json() {
                return {
                    success: true,
                    data: {
                        text: "整理一下这个需求",
                        provider: "openai-compatible",
                        language: "zh",
                        durationMs: 900,
                        requestId: "req-1"
                    }
                };
            }
        };
    },
    navigator: {
        mediaDevices: {
            getUserMedia: async () => ({})
        }
    },
    MediaRecorder: function MockMediaRecorder() {},
    calls: []
};

const voiceModule = vm.runInNewContext(
    `${serviceSource.replaceAll("export ", "")}\n({ isVoiceInputSupported, transcribeAudio });`,
    context
);
const { transcribeAudio } = voiceModule;

const result = await transcribeAudio(new Blob(["voice"], { type: "audio/webm" }), { mode: "fill", language: "zh" });

assert.equal(context.calls[0].url, "/api/web/audio/transcriptions");
assert.equal(context.calls[0].options.method, "POST");
assert.ok(context.calls[0].options.body instanceof FormData);
assert.equal(result.text, "整理一下这个需求");
assert.equal(result.provider, "openai-compatible");
console.log("voice-input upload mapping test passed");

const supportContext = {
    navigator: {},
    MediaRecorder: undefined
};
const { isVoiceInputSupported } = vm.runInNewContext(
    `${serviceSource.replaceAll("export ", "")}\n({ isVoiceInputSupported, transcribeAudio });`,
    supportContext
);

assert.equal(isVoiceInputSupported(), false);
console.log("voice-input support detection test passed");
