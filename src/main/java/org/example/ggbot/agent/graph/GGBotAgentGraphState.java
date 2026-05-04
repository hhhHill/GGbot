package org.example.ggbot.agent.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.example.ggbot.agent.AgentState;
import org.example.ggbot.agent.execution.ExecutionResult;
import org.example.ggbot.agent.reflection.ReflectionAnalysis;

/**
 * LangGraph状态适配层
 * 由于LangGraph要求状态必须是可序列化的字符串引用，这个类做了一层代理
 * 把实际的业务状态AgentState存在内存存储中，只在图中传递引用
 */
public class GGBotAgentGraphState extends org.bsc.langgraph4j.state.AgentState {

    /** AgentState业务对象的引用键 */
    static final String DELEGATE_REF = "delegateRef";
    /** 上一次执行结果的引用键 */
    static final String LAST_EXECUTION_RESULT_REF = "lastExecutionResultRef";
    /** 上一次反思结果的引用键 */
    static final String LAST_REFLECTION_REF = "lastReflectionRef";

    /** 全局状态存储，保存实际的业务对象，键为引用字符串 */
    private static final ConcurrentMap<String, Object> VALUE_STORE = new ConcurrentHashMap<>();
    private static final Map<String, Channel<?>> CHANNELS = Map.of(
            DELEGATE_REF, Channels.base(() -> ""),
            LAST_EXECUTION_RESULT_REF, Channels.base(() -> ""),
            LAST_REFLECTION_REF, Channels.base(() -> "")
    );

    public GGBotAgentGraphState(Map<String, Object> data) {
        super(data);
    }

    public static Map<String, Channel<?>> channels() {
        return CHANNELS;
    }

    public static Map<String, Object> inputOf(AgentState delegate) {
        String scope = UUID.randomUUID().toString();
        String delegateRef = scope + ":delegate";
        String executionRef = scope + ":execution";
        String reflectionRef = scope + ":reflection";

        VALUE_STORE.put(delegateRef, delegate);

        Map<String, Object> input = new HashMap<>();
        input.put(DELEGATE_REF, delegateRef);
        input.put(LAST_EXECUTION_RESULT_REF, executionRef);
        input.put(LAST_REFLECTION_REF, reflectionRef);
        return input;
    }

    public static void release(Map<String, Object> data) {
        removeRef(data.get(DELEGATE_REF));
        removeRef(data.get(LAST_EXECUTION_RESULT_REF));
        removeRef(data.get(LAST_REFLECTION_REF));
    }

    public AgentState delegate() {
        AgentState delegate = resolve(DELEGATE_REF, AgentState.class);
        if (delegate == null) {
            throw new IllegalStateException("Missing agent state delegate for LangGraph execution.");
        }
        return delegate;
    }

    public ExecutionResult lastExecutionResult() {
        return resolve(LAST_EXECUTION_RESULT_REF, ExecutionResult.class);
    }

    public ReflectionAnalysis lastReflection() {
        return resolve(LAST_REFLECTION_REF, ReflectionAnalysis.class);
    }

    public Map<String, Object> rememberExecutionResult(ExecutionResult result) {
        return remember(LAST_EXECUTION_RESULT_REF, result);
    }

    public Map<String, Object> rememberReflection(ReflectionAnalysis reflection) {
        return remember(LAST_REFLECTION_REF, reflection);
    }

    private Map<String, Object> remember(String channelName, Object value) {
        String ref = value(channelName).map(String.class::cast).orElse("");
        if (ref.isBlank()) {
            throw new IllegalStateException("Missing graph state reference for channel " + channelName + ".");
        }
        if (value == null) {
            VALUE_STORE.remove(ref);
        } else {
            VALUE_STORE.put(ref, value);
        }
        return Map.of(channelName, ref);
    }

    private <T> T resolve(String channelName, Class<T> expectedType) {
        String ref = value(channelName).map(String.class::cast).orElse("");
        if (ref.isBlank()) {
            return null;
        }
        Object value = VALUE_STORE.get(ref);
        if (value == null) {
            return null;
        }
        if (!expectedType.isInstance(value)) {
            throw new IllegalStateException("Unexpected value type for channel " + channelName + ".");
        }
        return expectedType.cast(value);
    }

    private static void removeRef(Object ref) {
        if (ref instanceof String refKey && !refKey.isBlank()) {
            VALUE_STORE.remove(refKey);
        }
    }
}
