package org.example.ggbot.adapter.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.example.ggbot.adapter.web.dto.WebAudioTranscriptionResponse;
import org.example.ggbot.asr.AsrService;
import org.example.ggbot.asr.AudioTranscriptionRequest;
import org.example.ggbot.asr.AudioTranscriptionResult;
import org.example.ggbot.asr.AudioUploadValidator;
import org.example.ggbot.common.ApiResponse;
import org.example.ggbot.exception.BadRequestException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/web/audio")
public class WebAudioTranscriptionController {

    private final AudioUploadValidator audioUploadValidator;
    private final AsrService asrService;

    public WebAudioTranscriptionController(AudioUploadValidator audioUploadValidator, AsrService asrService) {
        this.audioUploadValidator = audioUploadValidator;
        this.asrService = asrService;
    }

    @PostMapping(value = "/transcriptions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<WebAudioTranscriptionResponse> transcribe(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "mode", required = false, defaultValue = "fill") String mode,
            @RequestParam(value = "language", required = false, defaultValue = "zh") String language
    ) {
        try {
            audioUploadValidator.validate(file);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(exception.getMessage());
        }

        AudioTranscriptionResult result = asrService.transcribe(
                new AudioTranscriptionRequest(file, language));
        return ApiResponse.success(WebAudioTranscriptionResponse.from(result));
    }
}
