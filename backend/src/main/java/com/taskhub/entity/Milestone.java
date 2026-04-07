package com.taskhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("milestones")
public class Milestone {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String milestoneId;
    private String projectId;
    private String name;
    private LocalDate targetDate;
    private String description;
    private String color;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
