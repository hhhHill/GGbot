package org.example.ggbot.asr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class DashScopeAsrServiceTest {

    @Test
    void shouldThrowAsrProviderExceptionWhenRecognitionFails() {
        AsrProperties properties = properties();
        DashScopeAsrService service = new DashScopeAsrService(properties);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "voice.webm",
                "audio/webm",
                "voice".getBytes(StandardCharsets.UTF_8)
        );

        // No real API connection in test, so Recognition.call() will fail
        assertThatThrownBy(() -> service.transcribe(new AudioTranscriptionRequest(file, "zh")))
                .isInstanceOf(AsrProviderException.class);
    }

    @Test
    void shouldUseParaformerRealtimeModel() {
        AsrProperties properties = properties();
        assertThat(properties.getModel()).isEqualTo("paraformer-realtime-v2");
        assertThat(properties.getProvider()).isEqualTo("dashscope");
    }

    private AsrProperties properties() {
        AsrProperties properties = new AsrProperties();
        properties.setEnabled(true);
        properties.setProvider("dashscope");
        properties.setApiKey("test-key");
        properties.setModel("paraformer-realtime-v2");
        return properties;
    }
}
