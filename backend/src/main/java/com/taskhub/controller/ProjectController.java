package com.taskhub.controller;

import com.taskhub.dto.ProjectCreateDTO;
import com.taskhub.entity.Project;
import com.taskhub.entity.ProjectMember;
import com.taskhub.service.ProjectService;
import com.taskhub.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ApiResponse<Project> create(@RequestBody ProjectCreateDTO dto) {
        return ApiResponse.success(projectService.create(dto));
    }

    @GetMapping
    public ApiResponse<List<Project>> list() {
        return ApiResponse.success(projectService.list());
    }

    @GetMapping("/{projectId}")
    public ApiResponse<Project> get(@PathVariable String projectId) {
        Project project = projectService.getById(projectId);
        if (project == null) {
            return ApiResponse.error(404, "Project not found");
        }
        return ApiResponse.success(project);
    }

    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> delete(@PathVariable String projectId) {
        projectService.delete(projectId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{projectId}/members")
    public ApiResponse<List<ProjectMember>> members(@PathVariable String projectId) {
        return ApiResponse.success(projectService.getMembers(projectId));
    }

    @PostMapping("/{projectId}/members")
    public ApiResponse<Void> addMember(@PathVariable String projectId,
                                        @RequestParam String memberId,
                                        @RequestParam(defaultValue = "member") String role) {
        projectService.addMember(projectId, memberId, role);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    public ApiResponse<Void> removeMember(@PathVariable String projectId, @PathVariable String memberId) {
        projectService.removeMember(projectId, memberId);
        return ApiResponse.success(null);
    }
}
