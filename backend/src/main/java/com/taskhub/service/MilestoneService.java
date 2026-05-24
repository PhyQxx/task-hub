package com.taskhub.service;

import com.taskhub.dto.MilestoneCreateDTO;
import com.taskhub.entity.Milestone;
import com.taskhub.mapper.MilestoneMapper;
import com.taskhub.util.TaskIdGenerator;
import com.taskhub.ws.GanttWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MilestoneService {

    private final MilestoneMapper milestoneMapper;
    private final TaskIdGenerator taskIdGenerator;
    private final GanttWebSocketHandler wsHandler;

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
        // 广播里程碑创建事件
        wsHandler.broadcastTaskUpdate(
            dto.getProjectId(),
            "milestone:" + milestone.getMilestoneId(),
            Map.of("milestoneCreated", true, "name", milestone.getName()),
            createdBy
        );
        return milestone;
    }

    public List<Milestone> listByProject(String projectId) {
        return milestoneMapper.selectByProjectId(projectId);
    }

    public void delete(String milestoneId) {
        // 先查出 projectId 用于广播
        Milestone milestone = milestoneMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Milestone>()
                .eq("milestone_id", milestoneId)
        );
        milestoneMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Milestone>()
                .eq("milestone_id", milestoneId)
        );
        if (milestone != null) {
            wsHandler.broadcastTaskDeleted(
                milestone.getProjectId(),
                "milestone:" + milestoneId,
                "system"
            );
        }
    }
}
