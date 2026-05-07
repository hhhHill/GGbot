package org.example.ggbot.asr;

import java.time.Duration;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Data
@ConfigurationProperties(prefix = "asr")
public class AsrProperties {

    private boolean enabled = false;
    private String provider = "dashscope";
    private String baseUrl;
    private String apiKey;
    private String model = "paraformer-realtime-v2";
    private Duration timeout = Duration.ofSeconds(20);
    private DataSize minFileSize = DataSize.ofKilobytes(1);
    private DataSize maxFileSize = DataSize.ofMegabytes(5);
    private List<String> allowedContentTypes = List.of("audio/webm", "audio/mp4", "audio/mpeg", "audio/wav", "audio/ogg");
}
