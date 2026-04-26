package org.example.ggbot.planner;

/**
 * 表示规划层输出的最终意图类型。
 *
 * <p>它是 `PlannerService` 对多个规则命中结果做归并后的结论，
 * 上层会根据这个枚举理解当前请求属于哪一类任务。
 */
public enum IntentType {
    CHAT,
    CREATE_DOC,
    CREATE_PPT,
    CREATE_DOC_AND_PPT
}
