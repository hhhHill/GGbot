package org.example.ggbot.adapter.web.dto;

import org.example.ggbot.asr.AudioTranscriptionResult;

public record WebAudioTranscriptionResponse(
        String text,
        String provider,
        String language,
        Long durationMs,
        String requestId
) {

    public static WebAudioTranscriptionResponse from(AudioTranscriptionResult result) {
        return new WebAudioTranscriptionResponse(
                result.text(),
                result.provider(),
                result.language(),
                result.durationMs(),
                result.requestId()
        );
    }
}
