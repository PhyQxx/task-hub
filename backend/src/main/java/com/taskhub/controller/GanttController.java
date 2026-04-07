package com.taskhub.controller;

import com.taskhub.service.GanttService;
import com.taskhub.vo.ApiResponse;
import com.taskhub.vo.GanttDataVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gantt")
@RequiredArgsConstructor
public class GanttController {

    private final GanttService ganttService;

    @GetMapping("/project/{projectId}")
    public ApiResponse<GanttDataVO> getGanttData(@PathVariable String projectId) {
        return ApiResponse.success(ganttService.getGanttData(projectId));
    }
}
