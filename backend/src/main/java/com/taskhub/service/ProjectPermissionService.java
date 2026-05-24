package com.taskhub.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.taskhub.entity.ProjectMember;
import com.taskhub.mapper.ProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectPermissionService {

    private final ProjectMemberMapper projectMemberMapper;

    /**
     * 获取用户在项目中的角色
     * @return owner/member/viewer，或 null（非项目成员）
     */
    public String getRole(String projectId, String userId) {
        ProjectMember pm = projectMemberMapper.selectOne(
            new QueryWrapper<ProjectMember>()
                .eq("project_id", projectId)
                .eq("member_id", userId)
        );
        return pm != null ? pm.getRole() : null;
    }

    /**
     * 用户是否是项目成员（owner/member/viewer）
     */
    public boolean isMember(String projectId, String userId) {
        return getRole(projectId, userId) != null;
    }

    /**
     * 用户是否是项目 owner 或管理员角色
     */
    public boolean isOwnerOrAdmin(String projectId, String userId, boolean isGlobalAdmin) {
        if (isGlobalAdmin) return true;
        String role = getRole(projectId, userId);
        return "owner".equals(role);
    }

    /**
     * 用户是否可以编辑任务（owner 或 member）
     */
    public boolean canEdit(String projectId, String userId, boolean isGlobalAdmin) {
        if (isGlobalAdmin) return true;
        String role = getRole(projectId, userId);
        return "owner".equals(role) || "member".equals(role);
    }

    /**
     * 用户是否可以查看项目（任何角色）
     */
    public boolean canView(String projectId, String userId) {
        return isMember(projectId, userId);
    }
}
