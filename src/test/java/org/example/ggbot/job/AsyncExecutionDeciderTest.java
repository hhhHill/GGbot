package org.example.ggbot.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.example.ggbot.agent.AgentChannel;
import org.example.ggbot.agent.AgentRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class AsyncExecutionDeciderTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "请帮我导出聊天记录",
            "帮我整理一份项目方案文档",
            "做一个季度汇报PPT",
            "生成一份周会汇报"
    })
    void shouldUseAsyncForLongRunningRequests(String userInput) {
        AsyncExecutionDecider decider = new AsyncExecutionDecider();

        AsyncExecutionMode mode = decider.decide(request(userInput));

        assertThat(mode).isEqualTo(AsyncExecutionMode.ASYNC);
    }

    @Test
    void shouldUseSyncForShortChatRequests() {
        AsyncExecutionDecider decider = new AsyncExecutionDecider();

        AsyncExecutionMode mode = decider.decide(request("你好，今天帮我看下这个想法行不行？"));

        assertThat(mode).isEqualTo(AsyncExecutionMode.SYNC);
    }

    private AgentRequest request(String userInput) {
        return new AgentRequest(
                "conversation-1",
                "user-1",
                userInput,
                AgentChannel.WEB,
                null,
                "conversation-1",
                Map.of()
        );
    }
}
