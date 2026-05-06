package org.example.ggbot.asr;

import org.springframework.web.multipart.MultipartFile;

public record AudioTranscriptionRequest(
        MultipartFile file,
        String language
) {
}
