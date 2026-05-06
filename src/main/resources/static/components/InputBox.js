import React, { useEffect, useRef } from "react";
import { html } from "../lib/html.js";
import { isVoiceInputSupported, transcribeAudio } from "../services/voice-input.js";

export function InputBox({
    sending,
    error,
    value,
    onChange,
    onSend,
    quickActions,
    voiceState = "idle",
    voiceMode = "fill",
    onVoiceModeChange,
    onVoiceStateChange,
    onVoiceResult
}) {
    const textareaRef = useRef(null);
    const mediaRecorderRef = useRef(null);
    const audioChunksRef = useRef([]);
    const streamRef = useRef(null);

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

    async function handleVoiceToggle() {
        if (sending || voiceState === "transcribing") {
            return;
        }
        if (voiceState === "recording") {
            mediaRecorderRef.current?.stop();
            return;
        }
        if (!isVoiceInputSupported()) {
            onVoiceStateChange?.("failed", "当前浏览器不支持语音输入");
            return;
        }
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const recorder = new MediaRecorder(stream);
            streamRef.current = stream;
            mediaRecorderRef.current = recorder;
            audioChunksRef.current = [];
            recorder.ondataavailable = (event) => {
                if (event.data && event.data.size > 0) {
                    audioChunksRef.current.push(event.data);
                }
            };
            recorder.onstop = async () => {
                try {
                    onVoiceStateChange?.("transcribing", "");
                    const blob = new Blob(audioChunksRef.current, { type: recorder.mimeType || "audio/webm" });
                    const result = await transcribeAudio(blob, { mode: voiceMode, language: "zh" });
                    await onVoiceResult?.(voiceMode, result.text);
                    onVoiceStateChange?.("idle", "");
                } catch (voiceError) {
                    onVoiceStateChange?.("failed", voiceError.message || "语音转写失败");
                } finally {
                    streamRef.current?.getTracks()?.forEach((track) => track.stop());
                    streamRef.current = null;
                    mediaRecorderRef.current = null;
                    audioChunksRef.current = [];
                }
            };
            recorder.start();
            onVoiceStateChange?.("recording", "");
        } catch (voiceError) {
            onVoiceStateChange?.("failed", voiceError.message || "未获得麦克风权限");
        }
    }

    function voiceStatusText() {
        if (voiceState === "recording") {
            return "录音中，再点一次结束";
        }
        if (voiceState === "transcribing") {
            return "语音转写中...";
        }
        if (voiceState === "failed") {
            return "语音输入失败";
        }
        return `语音模式：${voiceMode === "send" ? "直接发送" : "回填输入框"}`;
    }

    return html`
        <div className="composer-inner">
            ${quickActions ? html`<div className="composer-quick-actions">${quickActions}</div>` : null}
            <form className="composer-panel" onSubmit=${handleSubmit}>
                <div className="composer-shell">
                    <div className="composer-tools">
                        <div className="voice-mode-toggle" role="group" aria-label="语音发送模式">
                            <button
                                className=${`voice-mode-button${voiceMode === "fill" ? " active" : ""}`}
                                type="button"
                                onClick=${() => onVoiceModeChange?.("fill")}
                            >
                                回填
                            </button>
                            <button
                                className=${`voice-mode-button${voiceMode === "send" ? " active" : ""}`}
                                type="button"
                                onClick=${() => onVoiceModeChange?.("send")}
                            >
                                直发
                            </button>
                        </div>
                        <button
                            className=${`chat-secondary-button voice-button${voiceState === "recording" ? " recording" : ""}${voiceState === "transcribing" ? " pending" : ""}`}
                            type="button"
                            onClick=${handleVoiceToggle}
                            disabled=${sending || voiceState === "transcribing"}
                        >
                            ${voiceState === "recording" ? "结束录音" : voiceState === "transcribing" ? "转写中..." : "语音输入"}
                        </button>
                    </div>
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
                            <div className="voice-status">${voiceStatusText()}</div>
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
