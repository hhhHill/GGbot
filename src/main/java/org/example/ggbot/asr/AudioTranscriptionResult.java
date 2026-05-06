package org.example.ggbot.asr;

public record AudioTranscriptionResult(
        String text,
        String provider,
        String language,
        Long durationMs,
        String requestId
) {
}
