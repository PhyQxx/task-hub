package com.taskhub.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.taskhub.dto.WorkLogCreateDTO;
import com.taskhub.entity.Task;
import com.taskhub.entity.TaskWorkLog;
import com.taskhub.mapper.TaskMapper;
import com.taskhub.mapper.TaskWorkLogMapper;
import com.taskhub.util.TaskIdGenerator;
import com.taskhub.ws.GanttWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkLogService {

    private final TaskWorkLogMapper workLogMapper;
    private final TaskMapper taskMapper;
    private final TaskIdGenerator taskIdGenerator;
    private final GanttWebSocketHandler wsHandler;

    public TaskWorkLog create(WorkLogCreateDTO dto) {
        // Bug-001: userId 非空校验
        if (dto.getUserId() == null || dto.getUserId().trim().isEmpty()) {
            throw new IllegalArgumentException("userId 不能为空");
        }
        // Bug-002: 重复提交校验（UK: task_id + log_date）
        if (dto.getTaskId() != null && dto.getLogDate() != null) {
            TaskWorkLog existing = workLogMapper.selectByTaskAndDate(dto.getTaskId(), dto.getLogDate());
            if (existing != null) {
                throw new DuplicateKeyException("该日期工作日志已存在");
            }
        }
        TaskWorkLog log = new TaskWorkLog();
        log.setLogId(taskIdGenerator.nextWorkLogId());
        log.setTaskId(dto.getTaskId());
        log.setUserId(dto.getUserId().trim());
        log.setLogDate(dto.getLogDate());
        log.setTodayDone(dto.getTodayDone());
        log.setTomorrowPlan(dto.getTomorrowPlan());
        log.setCurrentStatus(dto.getCurrentStatus() != null ? dto.getCurrentStatus() : "正常");
        log.setBlockedReason(dto.getBlockedReason());
        log.setHoursSpent(dto.getHoursSpent() != null ? dto.getHoursSpent() : 0f);
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        workLogMapper.insert(log);
        // 广播工作日志创建事件
        if (dto.getTaskId() != null) {
            wsHandler.broadcastTaskUpdate(
                getProjectIdByTaskId(dto.getTaskId()),
                dto.getTaskId(),
                java.util.Map.of("workLogUpdated", true),
                dto.getUserId()
            );
        }
        return log;
    }

    private String getProjectIdByTaskId(String taskId) {
        Task task = taskMapper.selectOne(
            new QueryWrapper<Task>()
                .eq("task_id", taskId)
                .select("project_id")
        );
        return task != null ? task.getProjectId() : "";
    }

    public List<TaskWorkLog> getByTaskId(String taskId) {
        return workLogMapper.selectByTaskId(taskId);
    }

    public List<TaskWorkLog> listAll(LocalDate date, String userId) {
        return workLogMapper.selectAll(date, userId);
    }
}
