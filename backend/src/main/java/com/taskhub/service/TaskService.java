package com.taskhub.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.taskhub.dto.TaskCreateDTO;
import com.taskhub.dto.TaskUpdateDTO;
import com.taskhub.entity.Task;
import com.taskhub.entity.TaskDependency;
import com.taskhub.entity.TaskHistory;
import com.taskhub.mapper.TaskDependencyMapper;
import com.taskhub.mapper.TaskHistoryMapper;
import com.taskhub.mapper.TaskMapper;
import com.taskhub.util.TaskIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskMapper taskMapper;
    private final TaskDependencyMapper dependencyMapper;
    private final TaskHistoryMapper historyMapper;
    private final TaskIdGenerator taskIdGenerator;

    public Task create(TaskCreateDTO dto, String createdBy) {
        Task task = new Task();
        task.setTaskId(taskIdGenerator.nextTaskId());
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setProjectId(dto.getProjectId());
        task.setAssigneeId(dto.getAssigneeId());
        task.setStartDate(dto.getStartDate());
        task.setEndDate(dto.getEndDate());
        task.setDuration(dto.getDuration());
        task.setEstimatedHours(dto.getEstimatedHours());
        task.setActualHours(0f);
        task.setProgress(0);
        task.setStatus(dto.getStatus() != null ? dto.getStatus() : "pending");
        task.setPriority(dto.getPriority() != null ? dto.getPriority() : "P2");
        task.setIsMilestone(dto.getIsMilestone() != null ? dto.getIsMilestone() : 0);
        task.setMilestoneDate(dto.getMilestoneDate());
        task.setTags(dto.getTags() != null ? JSON.toJSONString(dto.getTags()) : null);
        task.setCreatedBy(createdBy);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);

        saveHistory(task.getTaskId(), createdBy, "create", "{}");
        return task;
    }

    public Task update(String taskId, TaskUpdateDTO dto, String operator) {
        Task task = getById(taskId);
        if (task == null) throw new RuntimeException("Task not found: " + taskId);

        if (dto.getTitle() != null) task.setTitle(dto.getTitle());
        if (dto.getDescription() != null) task.setDescription(dto.getDescription());
        if (dto.getAssigneeId() != null) task.setAssigneeId(dto.getAssigneeId());
        if (dto.getStartDate() != null) task.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) task.setEndDate(dto.getEndDate());
        if (dto.getDuration() != null) task.setDuration(dto.getDuration());
        if (dto.getEstimatedHours() != null) task.setEstimatedHours(dto.getEstimatedHours());
        if (dto.getProgress() != null) task.setProgress(dto.getProgress());
        if (dto.getStatus() != null) {
            String oldStatus = task.getStatus();
            task.setStatus(dto.getStatus());
            if (!oldStatus.equals(dto.getStatus())) {
                saveHistory(taskId, operator, "status_change",
                    JSON.toJSONString(Map.of("from", oldStatus, "to", dto.getStatus())));
            }
        }
        if (dto.getPriority() != null) task.setPriority(dto.getPriority());
        if (dto.getBlockedReason() != null) task.setBlockedReason(dto.getBlockedReason());
        if (dto.getIsMilestone() != null) task.setIsMilestone(dto.getIsMilestone());
        if (dto.getMilestoneDate() != null) task.setMilestoneDate(dto.getMilestoneDate());
        if (dto.getTags() != null) task.setTags(JSON.toJSONString(dto.getTags()));

        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        saveHistory(taskId, operator, "update", JSON.toJSONString(dto));
        return task;
    }

    public void delete(String taskId, String operator) {
        taskMapper.delete(new QueryWrapper<Task>().eq("task_id", taskId));
        dependencyMapper.delete(new QueryWrapper<TaskDependency>().eq("task_id", taskId).or().eq("depends_on", taskId));
        saveHistory(taskId, operator, "delete", "{}");
    }

    public Task getById(String taskId) {
        return taskMapper.selectOne(new QueryWrapper<Task>().eq("task_id", taskId));
    }

    public List<Task> listByProject(String projectId) {
        return taskMapper.selectByProjectId(projectId);
    }

    public List<Task> getDownstreamTasks(String taskId) {
        List<TaskDependency> deps = dependencyMapper.selectByDependsOn(taskId);
        if (deps == null || deps.isEmpty()) return Collections.emptyList();
        List<String> downstreamIds = deps.stream().map(TaskDependency::getTaskId).collect(Collectors.toList());
        List<Task> result = new ArrayList<>();
        for (String id : downstreamIds) {
            Task t = getById(id);
            if (t != null) result.add(t);
        }
        return result;
    }

    public List<Task> getUpstreamTasks(String taskId) {
        List<TaskDependency> deps = dependencyMapper.selectByTaskId(taskId);
        if (deps == null || deps.isEmpty()) return Collections.emptyList();
        List<String> upstreamIds = deps.stream().map(TaskDependency::getDependsOn).collect(Collectors.toList());
        List<Task> result = new ArrayList<>();
        for (String id : upstreamIds) {
            Task t = getById(id);
            if (t != null) result.add(t);
        }
        return result;
    }

    @Transactional
    public void addDependency(String taskId, String dependsOn, String type) {
        TaskDependency dep = new TaskDependency();
        dep.setTaskId(taskId);
        dep.setDependsOn(dependsOn);
        dep.setDependencyType(type != null ? type : "FS");
        dep.setCreatedAt(LocalDateTime.now());
        dependencyMapper.insert(dep);
    }

    public void removeDependency(String taskId, String dependsOn) {
        dependencyMapper.delete(
            new QueryWrapper<TaskDependency>()
                .eq("task_id", taskId)
                .eq("depends_on", dependsOn)
        );
    }

    public List<TaskDependency> getDependencies(String taskId) {
        return dependencyMapper.selectAllByTaskId(taskId);
    }

    public List<TaskHistory> getHistory(String taskId) {
        return historyMapper.selectByTaskId(taskId);
    }

    public void saveHistory(String taskId, String operator, String action, String detail) {
        TaskHistory history = new TaskHistory();
        history.setTaskId(taskId);
        history.setOperator(operator);
        history.setAction(action);
        history.setDetail(detail);
        history.setCreatedAt(LocalDateTime.now());
        historyMapper.insert(history);
    }

    public Map<String, Set<String>> buildDependencyGraph(List<Task> tasks) {
        Map<String, Set<String>> graph = new HashMap<>();
        for (Task task : tasks) {
            graph.putIfAbsent(task.getTaskId(), new HashSet<>());
        }
        for (Task task : tasks) {
            List<TaskDependency> deps = dependencyMapper.selectByTaskId(task.getTaskId());
            for (TaskDependency dep : deps) {
                graph.computeIfAbsent(dep.getTaskId(), k -> new HashSet<>()).add(dep.getDependsOn());
            }
        }
        return graph;
    }
}
