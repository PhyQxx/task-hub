package com.taskhub.controller;

import com.taskhub.dto.TaskCreateDTO;
import com.taskhub.dto.TaskUpdateDTO;
import com.taskhub.dto.DependencyDTO;
import com.taskhub.entity.Task;
import com.taskhub.entity.TaskDependency;
import com.taskhub.entity.TaskHistory;
import com.taskhub.service.TaskService;
import com.taskhub.vo.ApiResponse;
import com.taskhub.vo.TaskHistoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ApiResponse<Task> create(@RequestBody TaskCreateDTO dto,
                                    @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return ApiResponse.success(taskService.create(dto, userId));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<Task> get(@PathVariable String taskId) {
        Task task = taskService.getById(taskId);
        if (task == null) return ApiResponse.error(404, "Task not found");
        return ApiResponse.success(task);
    }

    @PutMapping("/{taskId}")
    public ApiResponse<Task> update(@PathVariable String taskId,
                                     @RequestBody TaskUpdateDTO dto,
                                     @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return ApiResponse.success(taskService.update(taskId, dto, userId));
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> delete(@PathVariable String taskId,
                                    @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        taskService.delete(taskId, userId);
        return ApiResponse.success(null);
    }

    @GetMapping("/project/{projectId}")
    public ApiResponse<List<Task>> listByProject(@PathVariable String projectId) {
        return ApiResponse.success(taskService.listByProject(projectId));
    }

    @GetMapping("/{taskId}/history")
    public ApiResponse<TaskHistoryVO> history(@PathVariable String taskId) {
        List<TaskHistory> histories = taskService.getHistory(taskId);
        TaskHistoryVO vo = new TaskHistoryVO();
        vo.setHistory(histories.stream().map(h -> {
            TaskHistoryVO.HistoryItemVO item = new TaskHistoryVO.HistoryItemVO();
            item.setAction(h.getAction());
            item.setOperator(h.getOperator());
            item.setDetail(h.getDetail());
            item.setCreatedAt(h.getCreatedAt());
            return item;
        }).collect(java.util.stream.Collectors.toList()));
        return ApiResponse.success(vo);
    }

    @PostMapping("/dependencies")
    public ApiResponse<Void> addDependency(@RequestBody DependencyDTO dto) {
        taskService.addDependency(dto.getTaskId(), dto.getDependsOn(), dto.getDependencyType());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/dependencies")
    public ApiResponse<Void> removeDependency(@RequestParam String taskId, @RequestParam String dependsOn) {
        taskService.removeDependency(taskId, dependsOn);
        return ApiResponse.success(null);
    }

    @GetMapping("/{taskId}/dependencies")
    public ApiResponse<List<TaskDependency>> getDependencies(@PathVariable String taskId) {
        return ApiResponse.success(taskService.getDependencies(taskId));
    }

    @GetMapping("/{taskId}/downstream")
    public ApiResponse<List<Task>> downstream(@PathVariable String taskId) {
        return ApiResponse.success(taskService.getDownstreamTasks(taskId));
    }

    @GetMapping("/{taskId}/upstream")
    public ApiResponse<List<Task>> upstream(@PathVariable String taskId) {
        return ApiResponse.success(taskService.getUpstreamTasks(taskId));
    }
}
