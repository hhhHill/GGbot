package org.example.ggbot.ai;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ContextAwareChatService {

    private final ReliableChatService delegate;
    private final MemoryManager memoryManager;

    public boolean isAvailable() {
        return delegate.isAvailable();
    }

    public String chat(String systemPrompt, String currentInput, AgentContext context) {
        if (context == null) {
            return delegate.chat(systemPrompt, currentInput);
        }
        String fullPrompt = memoryManager.buildPrompt(currentInput, context);
        String reply = delegate.chat(systemPrompt, fullPrompt);
        writeConversationTurn(context, currentInput, reply);
        return reply;
    }

    public Flux<String> stream(String systemPrompt, String currentInput, AgentContext context) {
        if (context == null) {
            return delegate.stream(systemPrompt, currentInput);
        }
        String fullPrompt = memoryManager.buildPrompt(currentInput, context);
        AtomicReference<StringBuilder> replyBuffer = new AtomicReference<>(new StringBuilder());
        AtomicBoolean persisted = new AtomicBoolean(false);
        return delegate.stream(systemPrompt, fullPrompt)
                .doOnNext(chunk -> {
                    if (chunk != null) {
                        replyBuffer.get().append(chunk);
                    }
                })
                .doOnComplete(() -> persistOnce(context, currentInput, replyBuffer.get().toString(), persisted));
        }

    private void persistOnce(AgentContext context, String currentInput, String reply, AtomicBoolean persisted) {
        if (persisted.compareAndSet(false, true)) {
            writeConversationTurn(context, currentInput, reply);
        }
    }

    private void writeConversationTurn(AgentContext context, String currentInput, String reply) {
        memoryManager.addUserMessage(context, currentInput);
        memoryManager.addAssistantMessage(context, reply);
    }
}
