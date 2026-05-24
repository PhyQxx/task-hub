package com.taskhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskhub.entity.TaskComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskCommentMapper extends BaseMapper<TaskComment> {

    @Select("SELECT * FROM task_comments WHERE task_id = #{taskId} ORDER BY created_at DESC")
    List<TaskComment> selectByTaskId(String taskId);
}
