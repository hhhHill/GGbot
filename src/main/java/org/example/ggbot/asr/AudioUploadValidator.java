package org.example.ggbot.asr;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
public class AudioUploadValidator {

    private final AsrProperties properties;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传音频不能为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new IllegalArgumentException("暂不支持该音频格式");
        }
        if (file.getSize() < properties.getMinFileSize().toBytes()) {
            throw new IllegalArgumentException("录音时间过短，请重试");
        }
        if (file.getSize() > properties.getMaxFileSize().toBytes()) {
            throw new IllegalArgumentException("音频文件超过大小限制");
        }
    }

    private boolean isAllowedContentType(String contentType) {
        MediaType mediaType = MediaType.parseMediaType(contentType);
        return properties.getAllowedContentTypes().stream()
                .map(MediaType::parseMediaType)
                .anyMatch(allowed -> allowed.includes(mediaType));
    }
}
