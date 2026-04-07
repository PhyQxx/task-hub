package com.taskhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("projects")
public class Project {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String projectId;
    private String name;
    private String description;
    private String ownerId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
