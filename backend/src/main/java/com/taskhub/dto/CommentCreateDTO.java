package com.taskhub.dto;

import lombok.Data;

@Data
public class CommentCreateDTO {
    private String taskId;
    private String content;
}
