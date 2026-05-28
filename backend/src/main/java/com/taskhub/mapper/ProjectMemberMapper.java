package com.taskhub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.taskhub.entity.ProjectMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {
    default boolean existsMember(String projectId, String memberId) {
        return selectCount(new LambdaQueryWrapper<ProjectMember>()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getMemberId, memberId)) > 0;
    }
}
