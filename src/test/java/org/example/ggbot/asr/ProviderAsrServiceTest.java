package org.example.ggbot.asr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProviderAsrServiceTest {

    @Test
    void shouldMapProviderResponseToAudioTranscriptionResult() {
        AsrProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProviderAsrService service = new ProviderAsrService(builder, properties);

        server.expect(requestTo("https://asr.example.com/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-key"))
                .andRespond(withSuccess("""
                        {"text":"你好 GGbot","language":"zh","duration_ms":640,"request_id":"req-1"}
                        """, MediaType.APPLICATION_JSON));

        AudioTranscriptionResult result = service.transcribe(request());

        assertThat(result.text()).isEqualTo("你好 GGbot");
        assertThat(result.provider()).isEqualTo("openai-compatible");
        assertThat(result.language()).isEqualTo("zh");
        assertThat(result.durationMs()).isEqualTo(640L);
        assertThat(result.requestId()).isEqualTo("req-1");
        server.verify();
    }

    @Test
    void shouldWrapProviderErrors() {
        AsrProperties properties = properties();
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ProviderAsrService service = new ProviderAsrService(builder, properties);

        server.expect(requestTo("https://asr.example.com/audio/transcriptions"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.transcribe(request()))
                .isInstanceOf(AsrProviderException.class)
                .hasMessage("语音转写服务暂时不可用");

        server.verify();
    }

    private AsrProperties properties() {
        AsrProperties properties = new AsrProperties();
        properties.setEnabled(true);
        properties.setProvider("openai-compatible");
        properties.setBaseUrl("https://asr.example.com");
        properties.setApiKey("test-key");
        properties.setModel("whisper-1");
        return properties;
    }

    private AudioTranscriptionRequest request() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "voice.webm",
                "audio/webm",
                "voice".getBytes(StandardCharsets.UTF_8)
        );
        return new AudioTranscriptionRequest(file, "zh");
    }
}
