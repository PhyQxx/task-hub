package com.taskhub.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("members")
public class Member {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String memberId;
    private String nickname;
    private String role;
    private String skills;
    private Float weeklyCapacity;
    private Integer isActive;
    private String avatar;
    private String email;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
