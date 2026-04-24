package org.example.ggbot.adapter.feishu;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.feishu")
public class FeishuProperties {

    private boolean enabled;
    private boolean mockSend;
    private String appId;
    private String appSecret;
    private String baseUrl;
    private String tokenPath;
    private String messagePath;
}
