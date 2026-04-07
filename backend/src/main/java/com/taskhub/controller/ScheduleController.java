package com.taskhub.controller;

import com.taskhub.dto.TaskReorderDTO;
import com.taskhub.dto.BatchScheduleDTO;
import com.taskhub.service.ScheduleService;
import com.taskhub.vo.ApiResponse;
import com.taskhub.vo.BatchScheduleResultVO;
import com.taskhub.vo.MemberLoadTrendVO;
import com.taskhub.vo.ReorderResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping("/tasks/reorder")
    public ApiResponse<ReorderResultVO> reorder(@RequestBody TaskReorderDTO dto) {
        try {
            ReorderResultVO result = scheduleService.reorder(
                dto.getTaskId(),
                dto.getStartDate(),
                dto.getEndDate(),
                dto.getAutoReschedule() != null && dto.getAutoReschedule()
            );
            return ApiResponse.success(result);
        } catch (RuntimeException e) {
            return ApiResponse.error(40021, e.getMessage());
        }
    }

    @PostMapping("/tasks/batch-schedule")
    public ApiResponse<BatchScheduleResultVO> batchSchedule(@RequestBody BatchScheduleDTO dto) {
        return ApiResponse.success(scheduleService.batchSchedule(
            dto.getTaskIds(),
            dto.getProjectId(),
            dto.getMode()
        ));
    }

    @GetMapping("/members/{memberId}/load-trend")
    public ApiResponse<MemberLoadTrendVO> loadTrend(@PathVariable String memberId) {
        return ApiResponse.success(scheduleService.getMemberLoadTrend(memberId));
    }
}
