package com.taskhub.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.taskhub.dto.ProjectCreateDTO;
import com.taskhub.entity.Project;
import com.taskhub.entity.ProjectMember;
import com.taskhub.mapper.ProjectMapper;
import com.taskhub.mapper.ProjectMemberMapper;
import com.taskhub.util.TaskIdGenerator;
import lombok.RequiredArgsConstructor;
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

    public Project create(ProjectCreateDTO dto) {
        Project project = new Project();
        project.setProjectId(taskIdGenerator.nextProjectId());
        project.setName(dto.getName());
        project.setDescription(dto.getDescription());
        project.setOwnerId(dto.getOwnerId());
        project.setStartDate(dto.getStartDate());
        project.setEndDate(dto.getEndDate());
        project.setStatus("planning");
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projectMapper.insert(project);

        // Add owner as member
        ProjectMember pm = new ProjectMember();
        pm.setProjectId(project.getProjectId());
        pm.setMemberId(dto.getOwnerId());
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
