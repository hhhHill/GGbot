package org.example.ggbot.agent.graph;

/**
 * 工作流路由决策枚举
 * 表示下一步要执行的节点方向
 */
public enum AgentRoutingDecision {
    /** 结束工作流 */
    END,
    /** 继续执行下一轮计划 */
    EXECUTE,
    /** 进入重规划节点 */
    REPLAN
}
