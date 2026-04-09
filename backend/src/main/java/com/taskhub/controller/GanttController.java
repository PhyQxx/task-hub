package com.taskhub.controller;

import com.taskhub.service.GanttService;
import com.taskhub.vo.ApiResponse;
import com.taskhub.vo.GanttDataVO;
import com.taskhub.vo.GanttLinkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gantt")
@RequiredArgsConstructor
public class GanttController {

    private final GanttService ganttService;

    @GetMapping("/all")
    public ApiResponse<GanttDataVO> getAllGanttData() {
        return ApiResponse.success(ganttService.getAllGanttData());
    }

    @GetMapping("/project/{projectId}")
    public ApiResponse<GanttDataVO> getGanttData(@PathVariable String projectId) {
        return ApiResponse.success(ganttService.getGanttData(projectId));
    }

    /** 获取项目所有依赖关系 */
    @GetMapping("/links/{projectId}")
    public ApiResponse<List<GanttLinkVO>> getLinks(@PathVariable String projectId) {
        return ApiResponse.success(ganttService.getLinksByProject(projectId));
    }

    /** 新增依赖关系 */
    @PostMapping("/links")
    public ApiResponse<GanttLinkVO> addLink(@RequestBody LinkDTO dto) {
        if (dto.getTaskId() == null || dto.getDependsOn() == null) {
            return ApiResponse.error(400, "taskId 和 dependsOn 不能为空");
        }
        GanttLinkVO link = ganttService.addLink(
                dto.getTaskId(), dto.getDependsOn(),
                dto.getDependencyType() != null ? dto.getDependencyType() : "FS");
        return ApiResponse.success(link);
    }

    // DTO
    public static class LinkDTO {
        private String taskId;
        private String dependsOn;
        private String dependencyType;
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public String getDependsOn() { return dependsOn; }
        public void setDependsOn(String dependsOn) { this.dependsOn = dependsOn; }
        public String getDependencyType() { return dependencyType; }
        public void setDependencyType(String dependencyType) { this.dependencyType = dependencyType; }
    }
}
