package com.taskhub.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class TaskUpdateDTO {
    private List<String> taskIds;
    private String title;
    private String description;
    private String assigneeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer duration;
    private Float estimatedHours;
    private Integer progress;
    private String status;
    private String priority;
    private String blockedReason;
    private Integer isMilestone;
    private LocalDate milestoneDate;
    private String tags;
}
