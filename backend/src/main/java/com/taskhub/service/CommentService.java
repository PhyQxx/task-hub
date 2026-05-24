package com.taskhub.service;

import com.taskhub.dto.CommentCreateDTO;
import com.taskhub.entity.TaskComment;
import com.taskhub.mapper.TaskCommentMapper;
import com.taskhub.util.TaskIdGenerator;
import com.taskhub.ws.GanttWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final TaskCommentMapper commentMapper;
    private final TaskIdGenerator taskIdGenerator;
    private final TaskService taskService;
    private final GanttWebSocketHandler wsHandler;

    public TaskComment create(CommentCreateDTO dto, String userId) {
        if (dto.getTaskId() == null || dto.getTaskId().trim().isEmpty()) {
            throw new IllegalArgumentException("taskId 不能为空");
        }
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("评论内容不能为空");
        }

        TaskComment comment = new TaskComment();
        comment.setCommentId(taskIdGenerator.nextCommentId());
        comment.setTaskId(dto.getTaskId());
        comment.setUserId(userId);
        comment.setContent(dto.getContent().trim());
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        commentMapper.insert(comment);

        // 广播评论事件
        var task = taskService.getById(dto.getTaskId());
        if (task != null) {
            wsHandler.broadcastTaskUpdate(
                task.getProjectId(),
                dto.getTaskId(),
                Map.of("commentAdded", true, "commentId", comment.getCommentId()),
                userId
            );
        }

        return comment;
    }

    public List<TaskComment> listByTaskId(String taskId) {
        return commentMapper.selectByTaskId(taskId);
    }

    public void delete(String commentId, String userId) {
        commentMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TaskComment>()
                .eq("comment_id", commentId)
                .eq("user_id", userId)
        );
    }
}
