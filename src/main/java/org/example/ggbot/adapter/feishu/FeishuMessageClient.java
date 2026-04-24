package org.example.ggbot.adapter.feishu;

import java.util.Map;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.adapter.feishu.dto.FeishuMessageSendRequest;
import org.example.ggbot.common.JsonUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Data
@RequiredArgsConstructor
public class FeishuMessageClient {

    private final RestClient restClient;
    private final FeishuTokenClient feishuTokenClient;
    private final FeishuProperties feishuProperties;
    private final JsonUtils jsonUtils;

    public void sendText(String chatId, String replyText) {
        if (!feishuProperties.isEnabled()) {
            return;
        }
        if (feishuProperties.isMockSend()) {
            System.out.println("Mock Feishu send to chatId=" + chatId + ", text=" + replyText);
            return;
        }

        String token = feishuTokenClient.getTenantAccessToken();
        if (token.isBlank()) {
            System.out.println("Skip Feishu send because tenant access token is empty.");
            return;
        }

        FeishuMessageSendRequest request = new FeishuMessageSendRequest(
                chatId,
                "text",
                jsonUtils.toJson(Map.of("text", replyText))
        );

        restClient.post()
                .uri(feishuProperties.getBaseUrl() + feishuProperties.getMessagePath() + "?receive_id_type=chat_id")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
