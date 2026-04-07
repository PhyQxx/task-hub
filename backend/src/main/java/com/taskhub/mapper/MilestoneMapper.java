package com.taskhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskhub.entity.Milestone;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MilestoneMapper extends BaseMapper<Milestone> {

    @Select("SELECT * FROM milestones WHERE project_id = #{projectId} ORDER BY target_date")
    List<Milestone> selectByProjectId(@Param("projectId") String projectId);
}
