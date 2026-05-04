package org.example.ggbot.agent.runner;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.RunnableConfig;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.graph.AgentGraphFactory;
import org.example.ggbot.agent.graph.GGBotAgentGraphState;
import org.springframework.stereotype.Component;

@Component
public class LangGraphAgentRunner {

    private final AgentGraphFactory agentGraphFactory;

    public LangGraphAgentRunner(AgentGraphFactory agentGraphFactory) {
        this.agentGraphFactory = agentGraphFactory;
    }

    public AgentState run(AgentState initialState, int maxIterations) {
        Map<String, Object> input = GGBotAgentGraphState.inputOf(initialState);
        try {
            return agentGraphFactory.compiledGraph(maxIterations)
                    .invoke(input)
                    .map(GGBotAgentGraphState::delegate)
                    .orElse(initialState);
        } finally {
            GGBotAgentGraphState.release(input);
        }
    }

    public AsyncGenerator.Cancellable<NodeOutput<GGBotAgentGraphState>> stream(AgentState initialState, int maxIterations) {
        Map<String, Object> input = GGBotAgentGraphState.inputOf(initialState);
        AsyncGenerator.Cancellable<NodeOutput<GGBotAgentGraphState>> delegate =
                agentGraphFactory.compiledGraph(maxIterations).stream(input, RunnableConfig.builder().build());
        return new CleanupAwareGenerator(delegate, input);
    }

    private static final class CleanupAwareGenerator implements AsyncGenerator.Cancellable<NodeOutput<GGBotAgentGraphState>> {

        private final AsyncGenerator.Cancellable<NodeOutput<GGBotAgentGraphState>> delegate;
        private final Map<String, Object> input;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private CleanupAwareGenerator(
                AsyncGenerator.Cancellable<NodeOutput<GGBotAgentGraphState>> delegate,
                Map<String, Object> input) {
            this.delegate = delegate;
            this.input = input;
        }

        @Override
        public AsyncGenerator.Data<NodeOutput<GGBotAgentGraphState>> next() {
            try {
                AsyncGenerator.Data<NodeOutput<GGBotAgentGraphState>> data = delegate.next();
                if (data.isDone() || data.isError()) {
                    release();
                }
                return data;
            } catch (RuntimeException | Error ex) {
                release();
                throw ex;
            }
        }

        @Override
        public java.util.concurrent.Executor executor() {
            return delegate.executor();
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            try {
                return delegate.cancel(mayInterruptIfRunning);
            } finally {
                release();
            }
        }

        private void release() {
            if (released.compareAndSet(false, true)) {
                GGBotAgentGraphState.release(input);
            }
        }
    }
}
