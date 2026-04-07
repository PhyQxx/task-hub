package com.taskhub.controller;

import com.taskhub.dto.WorkLogCreateDTO;
import com.taskhub.entity.TaskWorkLog;
import com.taskhub.service.WorkLogService;
import com.taskhub.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/work-logs")
@RequiredArgsConstructor
public class WorkLogController {

    private final WorkLogService workLogService;

    @PostMapping
    public ApiResponse<TaskWorkLog> create(@RequestBody WorkLogCreateDTO dto) {
        return ApiResponse.success(workLogService.create(dto));
    }

    @GetMapping("/task/{taskId}")
    public ApiResponse<List<TaskWorkLog>> getByTask(@PathVariable String taskId) {
        return ApiResponse.success(workLogService.getByTaskId(taskId));
    }
}
