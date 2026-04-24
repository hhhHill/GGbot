package org.example.ggbot.planner;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.agent.AgentRequest;
import org.example.ggbot.tool.ToolName;
import org.springframework.stereotype.Service;

@Service
@Data
@RequiredArgsConstructor
public class PlannerService {

    public Plan plan(AgentRequest request) {
        String input = request.getUserInput() == null ? "" : request.getUserInput();
        boolean needPpt = containsAny(input, "PPT", "ppt", "演示", "汇报");
        boolean needDoc = containsAny(input, "文档", "方案", "PRD", "prd");

        IntentType intentType;
        if (needDoc && needPpt) {
            intentType = IntentType.CREATE_DOC_AND_PPT;
        } else if (needDoc) {
            intentType = IntentType.CREATE_DOC;
        } else if (needPpt) {
            intentType = IntentType.CREATE_PPT;
        } else {
            intentType = IntentType.CHAT;
        }

        List<PlanStep> steps = new ArrayList<>();
        if (needDoc) {
            steps.add(new PlanStep(ToolName.GENERATE_DOC, "生成方案文档", input));
        }
        if (needPpt) {
            steps.add(new PlanStep(ToolName.GENERATE_PPT, "生成汇报 PPT 大纲", input));
        }
        if (!needDoc && !needPpt) {
            steps.add(new PlanStep(ToolName.SUMMARIZE, "生成直接回复", input));
        }

        return new Plan(intentType, needDoc, needPpt, steps);
    }

    private boolean containsAny(String input, String... keywords) {
        for (String keyword : keywords) {
            if (input.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
