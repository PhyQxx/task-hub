package com.taskhub.controller;

import com.taskhub.dto.CommentCreateDTO;
import com.taskhub.entity.TaskComment;
import com.taskhub.service.CommentService;
import com.taskhub.vo.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ApiResponse<TaskComment> create(@RequestBody CommentCreateDTO dto, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        return ApiResponse.success(commentService.create(dto, userId));
    }

    @GetMapping("/task/{taskId}")
    public ApiResponse<List<TaskComment>> listByTask(@PathVariable String taskId) {
        return ApiResponse.success(commentService.listByTaskId(taskId));
    }

    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> delete(@PathVariable String commentId, Principal principal) {
        String userId = principal != null ? principal.getName() : "anonymous";
        commentService.delete(commentId, userId);
        return ApiResponse.success(null);
    }
}
