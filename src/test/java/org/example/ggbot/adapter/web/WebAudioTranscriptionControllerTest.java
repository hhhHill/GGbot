package org.example.ggbot.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.example.ggbot.asr.AsrService;
import org.example.ggbot.asr.AudioTranscriptionResult;
import org.example.ggbot.asr.AudioUploadValidator;
import org.example.ggbot.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WebAudioTranscriptionControllerTest {

    @Test
    void shouldReturnTranscriptionResult() throws Exception {
        AsrService asrService = mock(AsrService.class);
        AudioUploadValidator validator = mock(AudioUploadValidator.class);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "voice.webm",
                "audio/webm",
                "voice".getBytes(StandardCharsets.UTF_8)
        );

        when(asrService.transcribe(any())).thenReturn(new AudioTranscriptionResult(
                "帮我整理这个需求",
                "openai-compatible",
                "zh",
                1820L,
                "asr_123"
        ));

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new WebAudioTranscriptionController(validator, asrService)
        ).build();

        mockMvc.perform(multipart("/api/web/audio/transcriptions")
                        .file(file)
                        .param("mode", "fill")
                        .param("language", "zh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").value("帮我整理这个需求"))
                .andExpect(jsonPath("$.data.provider").value("openai-compatible"));
    }

    @Test
    void shouldRejectEmptyAudioFile() throws Exception {
        AsrService asrService = mock(AsrService.class);
        AudioUploadValidator validator = mock(AudioUploadValidator.class);
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.webm", "audio/webm", new byte[0]);

        doThrow(new IllegalArgumentException("上传音频不能为空")).when(validator).validate(any());

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new WebAudioTranscriptionController(validator, asrService)
        ).setControllerAdvice(new GlobalExceptionHandler()).build();

        mockMvc.perform(multipart("/api/web/audio/transcriptions").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("上传音频不能为空"));
    }
}
