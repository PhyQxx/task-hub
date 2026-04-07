package com.taskhub.service;

import com.taskhub.dto.MilestoneCreateDTO;
import com.taskhub.entity.Milestone;
import com.taskhub.mapper.MilestoneMapper;
import com.taskhub.util.TaskIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneMapper milestoneMapper;
    private final TaskIdGenerator taskIdGenerator;

    public Milestone create(MilestoneCreateDTO dto, String createdBy) {
        Milestone milestone = new Milestone();
        milestone.setMilestoneId(taskIdGenerator.nextMilestoneId());
        milestone.setProjectId(dto.getProjectId());
        milestone.setName(dto.getName());
        milestone.setTargetDate(dto.getTargetDate());
        milestone.setDescription(dto.getDescription());
        milestone.setColor(dto.getColor() != null ? dto.getColor() : "#FFD700");
        milestone.setCreatedBy(createdBy);
        milestone.setCreatedAt(LocalDateTime.now());
        milestone.setUpdatedAt(LocalDateTime.now());
        milestoneMapper.insert(milestone);
        return milestone;
    }

    public List<Milestone> listByProject(String projectId) {
        return milestoneMapper.selectByProjectId(projectId);
    }

    public void delete(String milestoneId) {
        milestoneMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Milestone>()
                .eq("milestone_id", milestoneId)
        );
    }
}
