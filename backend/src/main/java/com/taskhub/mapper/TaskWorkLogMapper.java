package com.taskhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskhub.entity.TaskWorkLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TaskWorkLogMapper extends BaseMapper<TaskWorkLog> {

    @Select("SELECT * FROM task_work_logs WHERE task_id = #{taskId} ORDER BY log_date DESC")
    List<TaskWorkLog> selectByTaskId(@Param("taskId") String taskId);

    @Select("SELECT * FROM task_work_logs WHERE user_id = #{userId} AND log_date = #{logDate}")
    TaskWorkLog selectByUserAndDate(@Param("userId") String userId, @Param("logDate") LocalDate logDate);
}
