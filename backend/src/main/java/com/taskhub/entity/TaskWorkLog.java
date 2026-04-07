package com.taskhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("task_work_logs")
public class TaskWorkLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String logId;
    private String taskId;
    private String userId;
    private LocalDate logDate;
    private String todayDone;
    private String tomorrowPlan;
    private String currentStatus;
    private String blockedReason;
    private Float hoursSpent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
