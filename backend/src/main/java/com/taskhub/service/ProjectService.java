package com.taskhub.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.taskhub.dto.ProjectCreateDTO;
import com.taskhub.dto.ProjectUpdateDTO;
import com.taskhub.entity.Project;
import com.taskhub.entity.ProjectMember;
import com.taskhub.mapper.ProjectMapper;
import com.taskhub.mapper.ProjectMemberMapper;
import com.taskhub.config.JwtAuthenticationFilter.LoginUser;
import com.taskhub.util.TaskIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final TaskIdGenerator taskIdGenerator;

    /**
     * 从 SecurityContext 获取当前登录用户的 memberId
     */
    private String getCurrentMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.memberId();
        }
        throw new IllegalStateException("无法获取当前用户信息");
    }

    public Project create(ProjectCreateDTO dto) {
        String ownerId = getCurrentMemberId();
        // 项目名校验
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        Project project = new Project();
        project.setProjectId(taskIdGenerator.nextProjectId());
        project.setName(dto.getName().trim());
        project.setDescription(dto.getDescription());
        project.setOwnerId(ownerId);
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setStatus("planning");
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.insert(project);

        // Add owner as member
        ProjectMember pm = new ProjectMember();
        pm.setProjectId(project.getProjectId());
        pm.setMemberId(ownerId);
        pm.setRole("owner");
        pm.setJoinedAt(LocalDateTime.now());
        projectMemberMapper.insert(pm);

        return project;
    }

    public List<Project> list() {
        return projectMapper.selectList(new QueryWrapper<>());
    }

    public Project getById(String projectId) {
        return projectMapper.selectOne(new QueryWrapper<Project>().eq("project_id", projectId));
    }

    public Project update(String projectId, ProjectUpdateDTO dto) {
        Project project = projectMapper.selectOne(new QueryWrapper<Project>().eq("project_id", projectId));
        if (project == null) throw new IllegalArgumentException("Project not found: " + projectId);
        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            project.setName(dto.getName().trim());
        }
        if (dto.getDescription() != null) project.setDescription(dto.getDescription());
        if (dto.getStartDate() != null) project.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) project.setEndDate(dto.getEndDate());
        if (dto.getStatus() != null) project.setStatus(dto.getStatus());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        return project;
    }

    public void delete(String projectId) {
        projectMapper.delete(new QueryWrapper<Project>().eq("project_id", projectId));
    }

    public List<ProjectMember> getMembers(String projectId) {
        return projectMemberMapper.selectList(
            new QueryWrapper<ProjectMember>().eq("project_id", projectId)
        );
    }

    @Transactional
    public void addMember(String projectId, String memberId, String role) {
        ProjectMember pm = new ProjectMember();
        pm.setProjectId(projectId);
        pm.setMemberId(memberId);
        pm.setRole(role);
        pm.setJoinedAt(LocalDateTime.now());
        projectMemberMapper.insert(pm);
    }

    public void removeMember(String projectId, String memberId) {
        projectMemberMapper.delete(
            new QueryWrapper<ProjectMember>()
                .eq("project_id", projectId)
                .eq("member_id", memberId)
        );
    }
}
