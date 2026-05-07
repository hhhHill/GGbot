package org.example.ggbot.asr;

import org.springframework.web.multipart.MultipartFile;

public record AudioTranscriptionRequest(
        MultipartFile file,
        String language,
        String fileUrl
) {
    public AudioTranscriptionRequest(MultipartFile file, String language) {
        this(file, language, null);
    }
}
