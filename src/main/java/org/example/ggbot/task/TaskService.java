package org.example.ggbot.task;

import java.util.Optional;

public interface TaskService {

    TaskRecord createTask(String conversationId, String userInput);

    Optional<TaskRecord> getTask(String taskId);

    void updateStatus(String taskId, TaskStatus status, String resultSummary);
}
