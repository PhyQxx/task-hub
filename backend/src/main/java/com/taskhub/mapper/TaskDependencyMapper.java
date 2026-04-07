package com.taskhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskhub.entity.TaskDependency;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskDependencyMapper extends BaseMapper<TaskDependency> {

    @Select("SELECT * FROM task_dependencies WHERE task_id = #{taskId}")
    List<TaskDependency> selectByTaskId(@Param("taskId") String taskId);

    @Select("SELECT * FROM task_dependencies WHERE depends_on = #{dependsOn}")
    List<TaskDependency> selectByDependsOn(@Param("dependsOn") String dependsOn);

    @Select("SELECT * FROM task_dependencies WHERE task_id = #{taskId} OR depends_on = #{taskId}")
    List<TaskDependency> selectAllByTaskId(@Param("taskId") String taskId);
}
