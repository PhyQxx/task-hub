package com.taskhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("project_members")
public class ProjectMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String projectId;
    private String memberId;
    private String role;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime joinedAt;
}
