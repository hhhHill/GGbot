package org.example.ggbot.agent.graph;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.springframework.stereotype.Component;

/**
 * LangGraph工作流工厂类
 * 负责构建和缓存Agent执行的有向无环图(DAG)
 */
@Component
public class AgentGraphFactory {

    /** 工作流起点节点 */
    private static final String START = "__START__";
    /** 工作流终点节点 */
    private static final String END = "__END__";
    /** 计划节点：生成执行计划 */
    private static final String PLAN = "plan";
    /** 执行节点：执行计划步骤 */
    private static final String EXECUTE = "execute";
    /** 反思节点：评估执行结果 */
    private static final String REFLECT = "reflect";
    /** 重规划节点：调整/重新生成计划 */
    private static final String REPLAN = "replan";

    /** 工作流节点实现 */
    private final AgentNodes agentNodes;
    /** 路由决策器：负责节点间的流转判断 */
    private final AgentGraphRouter router;
    /** 编译后的工作流缓存，key为最大迭代次数，value为编译好的图 */
    private final ConcurrentMap<Integer, CompiledGraph<GGBotAgentGraphState>> compiledGraphs = new ConcurrentHashMap<>();

    public AgentGraphFactory(AgentNodes agentNodes, AgentGraphRouter router) {
        this.agentNodes = agentNodes;
        this.router = router;
    }

    public CompiledGraph<GGBotAgentGraphState> compiledGraph(int maxIterations) {
        return compiledGraphs.computeIfAbsent(maxIterations, this::buildCompiledGraph);
    }

    private CompiledGraph<GGBotAgentGraphState> buildCompiledGraph(int maxIterations) {
        try {
            StateGraph<GGBotAgentGraphState> graph =
                    new StateGraph<>(GGBotAgentGraphState.channels(), GGBotAgentGraphState::new);

            graph.addNode(PLAN, AsyncNodeAction.node_async(agentNodes::plan));
            graph.addNode(EXECUTE, AsyncNodeAction.node_async(agentNodes::execute));
            graph.addNode(REFLECT, AsyncNodeAction.node_async(agentNodes::reflect));
            graph.addNode(REPLAN, AsyncNodeAction.node_async(agentNodes::replan));

            graph.addEdge(START, PLAN);
            graph.addEdge(PLAN, EXECUTE);
            graph.addEdge(EXECUTE, REFLECT);
            graph.addConditionalEdges(
                    REFLECT,
                    AsyncEdgeAction.edge_async(state -> router.routeAfterReflect(state, maxIterations).name()),
                    Map.of(
                            AgentRoutingDecision.END.name(), END,
                            AgentRoutingDecision.EXECUTE.name(), PLAN,
                            AgentRoutingDecision.REPLAN.name(), REPLAN
                    )
            );
            graph.addEdge(REPLAN, PLAN);

            CompiledGraph<GGBotAgentGraphState> compiledGraph = graph.compile();
            compiledGraph.setMaxIterations(maxIterations);
            return compiledGraph;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build LangGraph agent graph.", ex);
        }
    }
}
