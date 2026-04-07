package com.taskhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskhub.entity.TaskHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskHistoryMapper extends BaseMapper<TaskHistory> {

    @Select("SELECT * FROM task_history WHERE task_id = #{taskId} ORDER BY created_at DESC")
    List<TaskHistory> selectByTaskId(@Param("taskId") String taskId);
}
