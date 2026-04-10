package com.taskhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskhub.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Select("SELECT * FROM tasks WHERE project_id = #{projectId} ORDER BY start_date")
    List<Task> selectByProjectId(@Param("projectId") String projectId);

    @Select("SELECT * FROM tasks WHERE assignee_id = #{assigneeId} AND status != 'completed'")
    List<Task> selectActiveByAssigneeId(@Param("assigneeId") String assigneeId);

    @Select("SELECT * FROM tasks WHERE assignee_id = #{assigneeId}")
    List<Task> selectByAssigneeId(@Param("assigneeId") String assigneeId);

    @Select("SELECT * FROM tasks WHERE start_date >= #{startDate} AND end_date <= #{endDate} AND project_id = #{projectId}")
    List<Task> selectByProjectIdAndDateRange(@Param("projectId") String projectId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);
}
