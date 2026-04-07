package com.taskhub.service;

import com.alibaba.fastjson2.JSON;
import com.taskhub.entity.Member;
import com.taskhub.entity.Task;
import com.taskhub.entity.TaskDependency;
import com.taskhub.mapper.MemberMapper;
import com.taskhub.mapper.TaskMapper;
import com.taskhub.mapper.TaskDependencyMapper;
import com.taskhub.util.TopologicalSort;
import com.taskhub.vo.BatchScheduleResultVO;
import com.taskhub.vo.MemberLoadTrendVO;
import com.taskhub.vo.ReorderResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final TaskMapper taskMapper;
    private final MemberMapper memberMapper;
    private final TaskDependencyMapper dependencyMapper;
    private final TaskService taskService;

    public ReorderResultVO reorder(String taskId, LocalDate newStartDate, LocalDate newEndDate, boolean autoReschedule) {
        ReorderResultVO result = new ReorderResultVO();
        List<ReorderResultVO.RescheduledTaskVO> rescheduled = new ArrayList<>();

        Task task = taskService.getById(taskId);
        if (task == null) throw new RuntimeException("Task not found");

        int oldDuration = task.getDuration() != null ? task.getDuration() :
            (int) java.time.temporal.ChronoUnit.DAYS.between(task.getStartDate(), task.getEndDate()) + 1;
        int newDuration = (int) java.time.temporal.ChronoUnit.DAYS.between(newStartDate, newEndDate) + 1;
        int daysShift = newDuration - oldDuration;

        // Update the dragged task
        task.setStartDate(newStartDate);
        task.setEndDate(newEndDate);
        task.setDuration(newDuration);
        taskMapper.updateById(task);

        if (autoReschedule) {
            // Cascade downstream tasks
            List<Task> downstream = getDownstream(taskId, task.getProjectId());
            for (Task downstreamTask : downstream) {
                ReorderResultVO.RescheduledTaskVO item = new ReorderResultVO.RescheduledTaskVO();
                item.setTaskId(downstreamTask.getTaskId());
                item.setOldEndDate(downstreamTask.getEndDate());

                LocalDate newDownstreamStart = downstreamTask.getStartDate().plusDays(daysShift);
                LocalDate newDownstreamEnd = downstreamTask.getEndDate().plusDays(daysShift);
                item.setNewStartDate(newDownstreamStart);

                downstreamTask.setStartDate(newDownstreamStart);
                downstreamTask.setEndDate(newDownstreamEnd);
                taskMapper.updateById(downstreamTask);
                rescheduled.add(item);
            }
        }

        result.setRescheduled(rescheduled);
        result.setCascaded(rescheduled.size());
        return result;
    }

    private List<Task> getDownstream(String taskId, String projectId) {
        // Build dependency graph and topologically sort
        List<Task> allTasks = taskMapper.selectByProjectId(projectId);
        Map<String, Set<String>> graph = new HashMap<>();
        for (Task t : allTasks) {
            graph.putIfAbsent(t.getTaskId(), new HashSet<>());
        }
        for (Task t : allTasks) {
            List<TaskDependency> deps = dependencyMapper.selectByTaskId(t.getTaskId());
            for (TaskDependency dep : deps) {
                graph.computeIfAbsent(dep.getTaskId(), k -> new HashSet<>()).add(dep.getDependsOn());
            }
        }

        // BFS from taskId to find all reachable downstream tasks
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(taskId);
        visited.add(taskId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (TaskDependency dep : dependencyMapper.selectByDependsOn(current)) {
                if (!visited.contains(dep.getTaskId())) {
                    visited.add(dep.getTaskId());
                    queue.offer(dep.getTaskId());
                }
            }
        }

        return allTasks.stream()
            .filter(t -> visited.contains(t.getTaskId()) && !t.getTaskId().equals(taskId))
            .collect(Collectors.toList());
    }

    public BatchScheduleResultVO batchSchedule(List<String> taskIds, String projectId, String mode) {
        BatchScheduleResultVO result = new BatchScheduleResultVO();
        List<BatchScheduleResultVO.ScheduleItemVO> results = new ArrayList<>();

        List<Member> members = memberMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Member>()
                .eq("is_active", 1)
        );

        for (String taskId : taskIds) {
            Task task = taskService.getById(taskId);
            if (task == null) continue;

            BatchScheduleResultVO.ScheduleItemVO item = new BatchScheduleResultVO.ScheduleItemVO();
            item.setTaskId(taskId);

            if ("balance_load".equals(mode)) {
                Member best = findBestByLoad(members, task);
                if (best != null) {
                    item.setAssigneeId(best.getMemberId());
                    item.setNickname(best.getNickname());
                    item.setScore(calculateScore(best, task));
                    item.setReason("当前负载最低");
                }
            } else {
                // Default: pick first available
                if (!members.isEmpty()) {
                    Member m = members.get(0);
                    item.setAssigneeId(m.getMemberId());
                    item.setNickname(m.getNickname());
                    item.setScore(80);
                    item.setReason("默认分配");
                }
            }
            results.add(item);
        }

        result.setResults(results);
        return result;
    }

    private Member findBestByLoad(List<Member> members, Task task) {
        Member best = null;
        int minLoad = Integer.MAX_VALUE;
        for (Member m : members) {
            int load = calculateMemberLoad(m.getMemberId());
            if (load < minLoad) {
                minLoad = load;
                best = m;
            }
        }
        return best;
    }

    private int calculateMemberLoad(String memberId) {
        List<Task> activeTasks = taskMapper.selectActiveByAssigneeId(memberId);
        int totalHours = 0;
        for (Task t : activeTasks) {
            totalHours += t.getEstimatedHours() != null ? t.getEstimatedHours().intValue() : 8;
        }
        Member member = memberMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Member>()
                .eq("member_id", memberId)
        );
        float capacity = member != null && member.getWeeklyCapacity() != null ? member.getWeeklyCapacity() : 40f;
        return (int) (totalHours / capacity * 100);
    }

    private int calculateScore(Member member, Task task) {
        int loadScore = 100 - calculateMemberLoad(member.getMemberId());
        return Math.min(100, loadScore);
    }

    public MemberLoadTrendVO getMemberLoadTrend(String memberId) {
        MemberLoadTrendVO vo = new MemberLoadTrendVO();
        vo.setMemberId(memberId);

        LocalDate today = LocalDate.now();
        List<MemberLoadTrendVO.WeekLoadVO> weeks = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            LocalDate weekStart = today.plusWeeks(i).with(DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);

            List<Task> tasks = taskMapper.selectByProjectIdAndDateRange(null, weekStart, weekEnd);
            tasks = tasks.stream()
                .filter(t -> memberId.equals(t.getAssigneeId()))
                .collect(Collectors.toList());

            int totalHours = tasks.stream()
                .mapToInt(t -> t.getEstimatedHours() != null ? t.getEstimatedHours().intValue() : 8)
                .sum();

            Member member = memberMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Member>()
                    .eq("member_id", memberId)
            );
            float capacity = member != null && member.getWeeklyCapacity() != null ? member.getWeeklyCapacity() : 40f;
            int loadPct = (int) (totalHours / capacity * 100);

            MemberLoadTrendVO.WeekLoadVO weekVO = new MemberLoadTrendVO.WeekLoadVO();
            weekVO.setWeek("W" + weekStart.get(WeekFields.ISO.weekOfWeekBasedYear()));
            weekVO.setWeekLabel(weekStart + "~" + weekEnd);
            weekVO.setLoadPercent(Math.min(100, loadPct));
            weekVO.setTaskCount(tasks.size());
            weekVO.setLoadLevel(loadPct < 70 ? "normal" : loadPct < 90 ? "medium" : "high");
            weeks.add(weekVO);
        }

        vo.setWeeks(weeks);
        return vo;
    }
}
