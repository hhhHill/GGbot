package org.example.ggbot.task;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TaskRecord {

    private final String taskId;
    private final String conversationId;
    private final String userInput;
    private TaskStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String resultSummary;
}
