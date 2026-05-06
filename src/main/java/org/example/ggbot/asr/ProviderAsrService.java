package org.example.ggbot.asr;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
public class ProviderAsrService implements AsrService {

    private final RestClient restClient;
    private final AsrProperties properties;

    public ProviderAsrService(RestClient.Builder restClientBuilder, AsrProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .build();
    }

    @Override
    public AudioTranscriptionResult transcribe(AudioTranscriptionRequest request) {
        long startedAt = System.currentTimeMillis();
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new NamedByteArrayResource(request.file().getBytes(), request.file().getOriginalFilename()));
            body.add("model", properties.getModel());
            body.add("language", request.language());

            ProviderAsrResponse response = restClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(ProviderAsrResponse.class);

            if (response == null || response.text() == null || response.text().isBlank()) {
                throw new AsrProviderException("语音转写结果为空");
            }

            long latencyMs = System.currentTimeMillis() - startedAt;
            log.info(
                    "ASR transcription succeeded provider={} requestId={} latencyMs={} fileSize={} contentType={}",
                    properties.getProvider(),
                    response.requestId(),
                    latencyMs,
                    request.file().getSize(),
                    request.file().getContentType()
            );

            return new AudioTranscriptionResult(
                    response.text(),
                    properties.getProvider(),
                    response.language() == null || response.language().isBlank() ? request.language() : response.language(),
                    response.durationMs(),
                    response.requestId()
            );
        } catch (AsrProviderException exception) {
            throw exception;
        } catch (IOException exception) {
            log.warn(
                    "ASR transcription failed provider={} fileSize={} contentType={}",
                    properties.getProvider(),
                    request.file().getSize(),
                    request.file().getContentType(),
                    exception
            );
            throw new AsrProviderException("语音转写服务暂时不可用", exception);
        } catch (Exception exception) {
            log.warn(
                    "ASR transcription failed provider={} fileSize={} contentType={}",
                    properties.getProvider(),
                    request.file().getSize(),
                    request.file().getContentType(),
                    exception
            );
            throw new AsrProviderException("语音转写服务暂时不可用", exception);
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new AsrProviderException("语音转写服务未完成配置");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private record ProviderAsrResponse(
            String text,
            String language,
            @JsonProperty("duration_ms") Long durationMs,
            @JsonProperty("request_id") String requestId
    ) {
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {

        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename == null || filename.isBlank() ? "voice.webm" : filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}
