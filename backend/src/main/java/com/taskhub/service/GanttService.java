package com.taskhub.service;

import com.taskhub.entity.Member;
import com.taskhub.entity.Task;
import com.taskhub.entity.TaskDependency;
import com.taskhub.mapper.MemberMapper;
import com.taskhub.mapper.TaskMapper;
import com.taskhub.mapper.TaskDependencyMapper;
import com.taskhub.vo.GanttDataVO;
import com.taskhub.vo.GanttLinkVO;
import com.taskhub.vo.GanttTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GanttService {

    private final TaskMapper taskMapper;
    private final MemberMapper memberMapper;
    private final TaskDependencyMapper dependencyMapper;

    public GanttDataVO getGanttData(String projectId) {
        List<Task> tasks = taskMapper.selectByProjectId(projectId);
        List<TaskDependency> allDeps = new ArrayList<>();
        for (Task task : tasks) {
            allDeps.addAll(dependencyMapper.selectByTaskId(task.getTaskId()));
        }

        List<GanttTaskVO> taskVOs = tasks.stream().map(this::toGanttTaskVO).collect(Collectors.toList());
        List<GanttLinkVO> linkVOs = allDeps.stream().map(this::toGanttLinkVO).collect(Collectors.toList());

        GanttDataVO vo = new GanttDataVO();
        vo.setData(taskVOs);
        vo.setLinks(linkVOs);
        return vo;
    }

    public GanttDataVO getAllGanttData() {
        List<Task> tasks = taskMapper.selectList(null);
        List<TaskDependency> allDeps = dependencyMapper.selectList(null);

        List<GanttTaskVO> taskVOs = tasks.stream().map(this::toGanttTaskVO).collect(Collectors.toList());
        List<GanttLinkVO> linkVOs = allDeps.stream().map(this::toGanttLinkVO).collect(Collectors.toList());

        GanttDataVO vo = new GanttDataVO();
        vo.setData(taskVOs);
        vo.setLinks(linkVOs);
        return vo;
    }

    private GanttTaskVO toGanttTaskVO(Task task) {
        GanttTaskVO vo = new GanttTaskVO();
        vo.setId(task.getTaskId());
        vo.setText(task.getTitle());
        vo.setStart_date(task.getStartDate());
        vo.setDuration(calculateDuration(task.getStartDate(), task.getEndDate()));
        vo.setProgress(task.getProgress() != null ? task.getProgress() / 100f : 0f);
        vo.setStatus(task.getStatus());
        vo.setPriority(task.getPriority());
        vo.setDescription(task.getDescription());
        vo.setAssignee_id(task.getAssigneeId());
        vo.setProject_id(task.getProjectId());
        vo.setIs_milestone(task.getIsMilestone());
        vo.setMilestone_date(task.getMilestoneDate());

        if (task.getAssigneeId() != null) {
            Member member = memberMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Member>()
                    .eq("member_id", task.getAssigneeId())
            );
            if (member != null) {
                vo.setAssignee_name(member.getNickname());
            }
        }
        return vo;
    }

    private GanttLinkVO toGanttLinkVO(TaskDependency dep) {
        GanttLinkVO vo = new GanttLinkVO();
        vo.setId(dep.getId() + "");
        vo.setSource(dep.getDependsOn());
        vo.setTarget(dep.getTaskId());
        vo.setType(dep.getDependencyType());
        return vo;
    }

    public GanttLinkVO addLink(String taskId, String dependsOn, String dependencyType) {
        TaskDependency dep = new TaskDependency();
        dep.setTaskId(taskId);
        dep.setDependsOn(dependsOn);
        dep.setDependencyType(dependencyType);
        dep.setCreatedAt(java.time.LocalDateTime.now());
        dependencyMapper.insert(dep);
        GanttLinkVO vo = new GanttLinkVO();
        vo.setId(dep.getId() + "");
        vo.setSource(dependsOn);
        vo.setTarget(taskId);
        vo.setType(dependencyType);
        return vo;
    }

    public List<GanttLinkVO> getLinksByProject(String projectId) {
        List<Task> tasks = taskMapper.selectByProjectId(projectId);
        List<TaskDependency> allDeps = new ArrayList<>();
        for (Task task : tasks) {
            allDeps.addAll(dependencyMapper.selectByTaskId(task.getTaskId()));
        }
        return allDeps.stream().map(this::toGanttLinkVO).collect(Collectors.toList());
    }

    private Integer calculateDuration(java.time.LocalDate start, java.time.LocalDate end) {
        if (start == null || end == null) return 1;
        // gantt duration_unit=day 时，duration = end - start（不含末尾），所以这里也不 +1
        return (int) java.time.temporal.ChronoUnit.DAYS.between(start, end);
    }
}
