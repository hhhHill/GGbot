package org.example.ggbot.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;

class SpringAiConversationMemoryServiceTest {

    @Test
    void shouldStoreMessagesInChatMemoryAndExposeStringHistory() {
        SpringAiConversationMemoryService service =
                new SpringAiConversationMemoryService(MessageWindowChatMemory.builder().maxMessages(20).build());

        service.appendUserMessage("conversation-1", "你好");
        service.appendAgentMessage("conversation-1", "你好，我可以帮你生成方案");

        List<String> history = service.getConversationHistory("conversation-1");
        List<Message> messages = service.getMessages("conversation-1");

        assertThat(history).containsExactly(
                "USER: 你好",
                "AGENT: 你好，我可以帮你生成方案"
        );
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(messages.get(1).getMessageType()).isEqualTo(MessageType.ASSISTANT);
    }
}
