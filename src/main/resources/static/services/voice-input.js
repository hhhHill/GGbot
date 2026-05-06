export function isVoiceInputSupported() {
    return typeof MediaRecorder !== "undefined" && !!globalThis.navigator?.mediaDevices?.getUserMedia;
}

export async function transcribeAudio(blob, { mode = "fill", language = "zh" } = {}) {
    const formData = new FormData();
    formData.append("file", blob, "voice.webm");
    formData.append("mode", mode);
    formData.append("language", language);

    const response = await fetch("/api/web/audio/transcriptions", {
        method: "POST",
        body: formData
    });
    const payload = await response.json();
    if (!response.ok || !payload.success) {
        throw new Error(payload.message || "语音转写失败");
    }
    return payload.data;
}
