package org.example.ggbot.asr;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class AudioUploadValidatorTest {

    @Test
    void shouldRejectEmptyFile() {
        AudioUploadValidator validator = new AudioUploadValidator(properties());
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.webm", "audio/webm", new byte[0]);

        assertThatThrownBy(() -> validator.validate(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("上传音频不能为空");
    }

    @Test
    void shouldRejectUnsupportedAudioContentType() {
        AudioUploadValidator validator = new AudioUploadValidator(properties());
        MockMultipartFile file = new MockMultipartFile("file", "voice.txt", "text/plain", "bad".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("暂不支持该音频格式");
    }

    @Test
    void shouldAcceptWebmWithCodecsParameter() {
        AudioUploadValidator validator = new AudioUploadValidator(properties());
        MockMultipartFile file = new MockMultipartFile("file", "voice.webm", "audio/webm;codecs=opus", "audio".getBytes(StandardCharsets.UTF_8));

        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptOggContentType() {
        AudioUploadValidator validator = new AudioUploadValidator(properties());
        MockMultipartFile file = new MockMultipartFile("file", "voice.ogg", "audio/ogg", "audio".getBytes(StandardCharsets.UTF_8));

        assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectOversizeAudioFile() {
        AsrProperties properties = properties();
        properties.setMaxFileSize(DataSize.ofBytes(3));
        AudioUploadValidator validator = new AudioUploadValidator(properties);
        MockMultipartFile file = new MockMultipartFile("file", "voice.webm", "audio/webm", "oversize".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("音频文件超过大小限制");
    }

    @Test
    void shouldRejectTooSmallAudioFile() {
        AsrProperties properties = properties();
        properties.setMinFileSize(DataSize.ofBytes(128));
        AudioUploadValidator validator = new AudioUploadValidator(properties);
        MockMultipartFile file = new MockMultipartFile("file", "voice.webm", "audio/webm", "short".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("录音时间过短，请重试");
    }

    private AsrProperties properties() {
        AsrProperties properties = new AsrProperties();
        properties.setAllowedContentTypes(List.of("audio/webm", "audio/mpeg", "audio/ogg"));
        properties.setMaxFileSize(DataSize.ofMegabytes(5));
        properties.setMinFileSize(DataSize.ofBytes(1));
        return properties;
    }
}
