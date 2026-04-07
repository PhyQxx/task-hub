package com.taskhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("tasks")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String title;
    private String description;
    private String projectId;
    private String assigneeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer duration;
    private Float estimatedHours;
    private Float actualHours;
    private Integer progress;
    private String status;
    private String priority;
    private String blockedReason;
    private LocalDateTime blockedAt;
    private Integer isMilestone;
    private LocalDate milestoneDate;
    private String tags;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
