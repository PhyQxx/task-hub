package com.taskhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_history")
public class TaskHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskId;
    private String operator;
    private String action;
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
