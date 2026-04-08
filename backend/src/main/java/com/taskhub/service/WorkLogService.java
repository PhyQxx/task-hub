package com.taskhub.service;

import com.taskhub.dto.WorkLogCreateDTO;
import com.taskhub.entity.TaskWorkLog;
import com.taskhub.mapper.TaskWorkLogMapper;
import com.taskhub.util.TaskIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkLogService {

    private final TaskWorkLogMapper workLogMapper;
    private final TaskIdGenerator taskIdGenerator;

    public TaskWorkLog create(WorkLogCreateDTO dto) {
        TaskWorkLog log = new TaskWorkLog();
        log.setLogId(taskIdGenerator.nextWorkLogId());
        log.setTaskId(dto.getTaskId());
        log.setUserId(dto.getUserId());
        log.setLogDate(dto.getLogDate());
        log.setTodayDone(dto.getTodayDone());
        log.setTomorrowPlan(dto.getTomorrowPlan());
        log.setCurrentStatus(dto.getCurrentStatus() != null ? dto.getCurrentStatus() : "正常");
        log.setBlockedReason(dto.getBlockedReason());
        log.setHoursSpent(dto.getHoursSpent() != null ? dto.getHoursSpent() : 0f);
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        workLogMapper.insert(log);
        return log;
    }

    public List<TaskWorkLog> getByTaskId(String taskId) {
        return workLogMapper.selectByTaskId(taskId);
    }

    public List<TaskWorkLog> listAll(LocalDate date, String userId) {
        return workLogMapper.selectAll(date, userId);
    }
}
