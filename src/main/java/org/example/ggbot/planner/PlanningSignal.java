package org.example.ggbot.planner;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 规则层输出的中间模型。
 *
 * <p>它用于承接多个 `PlanningRule` 的匹配结果，例如是否需要文档、是否需要 PPT、
 * 命中了哪些关键词、由哪些规则贡献了结果。
 *
 * <p>`PlannerService` 会把多个 `PlanningSignal` 合并后，再计算最终的 `IntentType`。
 */
@Data
@RequiredArgsConstructor
public class PlanningSignal {

    private boolean needDoc;
    private boolean needPpt;
    private boolean needClarification;
    private String clarificationQuestion;
    private final List<String> matchedKeywords = new ArrayList<>();
    private final List<String> matchedRules = new ArrayList<>();

    /**
     * 将另一条规则产生的规划信号合并到当前信号中。
     */
    public void merge(PlanningSignal other) {
        this.needDoc = this.needDoc || other.needDoc;
        this.needPpt = this.needPpt || other.needPpt;
        this.needClarification = this.needClarification || other.needClarification;
        if ((this.clarificationQuestion == null || this.clarificationQuestion.isBlank())
                && other.clarificationQuestion != null && !other.clarificationQuestion.isBlank()) {
            this.clarificationQuestion = other.clarificationQuestion;
        }
        this.matchedKeywords.addAll(other.matchedKeywords);
        this.matchedRules.addAll(other.matchedRules);
    }

    /**
     * 创建一个不携带任何命中结果的空信号。
     */
    public static PlanningSignal empty() {
        return new PlanningSignal();
    }
}
