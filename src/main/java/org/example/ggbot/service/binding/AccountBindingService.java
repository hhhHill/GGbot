package org.example.ggbot.service.binding;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.exception.BadRequestException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountBindingService {

    private final Map<String, BindTokenPayload> bindTokens = new ConcurrentHashMap<>();

    public String createBindToken(Long userId, Long currentOrgId) {
        String token = UUID.randomUUID().toString();
        bindTokens.put(token, new BindTokenPayload(userId, currentOrgId, LocalDateTime.now().plusMinutes(10)));
        return token;
    }

    public void bindFeishuIdentity(String token, String openId, String tenantKey, String tenantName) {
        BindTokenPayload payload = bindTokens.get(token);
        if (payload == null || payload.expiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Bind token is invalid or expired");
        }

        // TODO: 通过 tenant_key 找到或创建飞书 organization。
        // TODO: 将 feishu identity 绑定到 payload.webUserId。
        // TODO: 确保 web 用户加入飞书 org，并处理 identity 已属于其他 user 的合并入口。
        throw new UnsupportedOperationException("bindFeishuIdentity is not implemented yet");
    }

    public void mergeUsers(Long sourceUserId, Long targetUserId) {
        // TODO: 迁移 user_identities.user_id、user_orgs.user_id、group_members.user_id、messages.sender_user_id。
        // TODO: 处理 source/target user 在同 org 下的 user subject，迁移其 conversations / memory 到 target subject。
        throw new UnsupportedOperationException("mergeUsers is not implemented yet");
    }

    private record BindTokenPayload(Long webUserId, Long currentOrgId, LocalDateTime expiresAt) {
    }
}
