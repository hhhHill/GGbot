package org.example.ggbot.agent.graph;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Agent工作流配置属性
 * 从配置文件读取agent.runner前缀的配置项
 */
@Data
@Validated
@ConfigurationProperties(prefix = "agent.runner")
public class AgentGraphProperties {

    /**
     * 最大迭代轮次，防止Agent无限循环
     * 每轮计划→执行→反思算一次迭代
     */
    @Min(1)
    private int maxIterations = 10;
}
