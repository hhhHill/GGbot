package org.example.ggbot.adapter.feishu;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Data
@RequiredArgsConstructor
public class FeishuTokenClient {

    private final RestClient restClient;
    private final FeishuProperties feishuProperties;

    public String getTenantAccessToken() {
        if (!feishuProperties.isEnabled() || feishuProperties.isMockSend()) {
            return "mock-feishu-token";
        }
        if (isBlank(feishuProperties.getAppId()) || isBlank(feishuProperties.getAppSecret())) {
            return "";
        }

        Map<?, ?> response = restClient.post()
                .uri(feishuProperties.getBaseUrl() + feishuProperties.getTokenPath())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "app_id", feishuProperties.getAppId(),
                        "app_secret", feishuProperties.getAppSecret()
                ))
                .retrieve()
                .body(Map.class);

        Object token = response == null ? null : response.get("tenant_access_token");
        return token == null ? "" : token.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
