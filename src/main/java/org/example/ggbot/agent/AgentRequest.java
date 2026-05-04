package org.example.ggbot.agent;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * Agent请求传输对象，封装所有调用Agent所需的信息
 * 是外部系统与Agent核心之间的请求契约
 */
@Data
public class AgentRequest {

    /** 会话ID，唯一标识一个对话 */
    private final String conversationId;
    /** 用户ID，发起请求的用户标识 */
    private final String userId;
    /** 用户输入内容，当前请求的用户问题/指令 */
    private final String userInput;
    private final AgentChannel channel;
    /** 请求渠道（WEB/FEISHU等） */
    private final String channelMessageId;
    /** 渠道侧消息ID，用于回调或去重 */
    private final String replyTargetId;
    /** 回复目标ID，如飞书消息ID用于@回复 */
    private final Map<String, Object> metadata;
    /** 扩展元数据，可存储自定义属性 */
    private final List<String> history;
    /** 会话历史列表，格式为"USER: xxx"/"AGENT: xxx" */
    private final List<String> memory;
    /** 全局记忆列表，用户的长期记忆信息 */

    /**
     * 简化构造函数，使用默认的空历史和空记忆
     */
    public AgentRequest(
            String conversationId,
            String userId,
            String userInput,
            AgentChannel channel,
            String channelMessageId,
            String replyTargetId,
            Map<String, Object> metadata
    ) {
        this(conversationId, userId, userInput, channel, channelMessageId, replyTargetId, metadata, List.of(), List.of());
    }

    /**
     * 全参数构造函数
     */
    public AgentRequest(
            String conversationId,
            String userId,
            String userInput,
            AgentChannel channel,
            String channelMessageId,
            String replyTargetId,
            Map<String, Object> metadata,
            List<String> history,
            List<String> memory
    ) {
        this.conversationId = conversationId;
        this.userId = userId;
        this.userInput = userInput;
        this.channel = channel;
        this.channelMessageId = channelMessageId;
        this.replyTargetId = replyTargetId;
        this.metadata = metadata == null ? Map.of() : metadata;
        this.history = history == null ? List.of() : history;
        this.memory = memory == null ? List.of() : memory;
    }
}
