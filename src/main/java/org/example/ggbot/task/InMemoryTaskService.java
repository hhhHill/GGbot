package org.example.ggbot.task;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.ggbot.common.IdGenerator;
import org.springframework.stereotype.Service;

@Service
@Data
@RequiredArgsConstructor
public class InMemoryTaskService implements TaskService {

    private final IdGenerator idGenerator;
    private final ConcurrentMap<String, TaskRecord> taskStore = new ConcurrentHashMap<>();

    @Override
    public TaskRecord createTask(String conversationId, String userInput) {
        LocalDateTime now = LocalDateTime.now();
        TaskRecord taskRecord = new TaskRecord(
                idGenerator.nextId("task"),
                conversationId,
                userInput,
                now
        );
        taskRecord.setStatus(TaskStatus.CREATED);
        taskRecord.setUpdatedAt(now);
        taskRecord.setResultSummary(null);
        taskStore.put(taskRecord.getTaskId(), taskRecord);
        return taskRecord;
    }

    @Override
    public Optional<TaskRecord> getTask(String taskId) {
        return Optional.ofNullable(taskStore.get(taskId));
    }

    @Override
    public void updateStatus(String taskId, TaskStatus status, String resultSummary) {
        TaskRecord taskRecord = taskStore.get(taskId);
        if (taskRecord == null) {
            return;
        }
        taskRecord.setStatus(status);
        taskRecord.setUpdatedAt(LocalDateTime.now());
        taskRecord.setResultSummary(resultSummary);
    }
}
