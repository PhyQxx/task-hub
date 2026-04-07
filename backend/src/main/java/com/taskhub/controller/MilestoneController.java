package com.taskhub.controller;

import com.taskhub.dto.MilestoneCreateDTO;
import com.taskhub.entity.Milestone;
import com.taskhub.service.MilestoneService;
import com.taskhub.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/milestones")
@RequiredArgsConstructor
public class MilestoneController {

    private final MilestoneService milestoneService;

    @PostMapping
    public ApiResponse<Milestone> create(@RequestBody MilestoneCreateDTO dto,
                                          @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {
        return ApiResponse.success(milestoneService.create(dto, userId));
    }

    @GetMapping("/project/{projectId}")
    public ApiResponse<List<Milestone>> listByProject(@PathVariable String projectId) {
        return ApiResponse.success(milestoneService.listByProject(projectId));
    }

    @DeleteMapping("/{milestoneId}")
    public ApiResponse<Void> delete(@PathVariable String milestoneId) {
        milestoneService.delete(milestoneId);
        return ApiResponse.success(null);
    }
}
