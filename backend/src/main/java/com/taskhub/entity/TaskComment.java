package com.taskhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_comments")
public class TaskComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String commentId;
    private String taskId;
    private String userId;
    private String content;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
