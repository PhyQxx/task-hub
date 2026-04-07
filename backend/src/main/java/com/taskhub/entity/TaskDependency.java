package com.taskhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_dependencies")
public class TaskDependency {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String dependsOn;
    private String dependencyType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
