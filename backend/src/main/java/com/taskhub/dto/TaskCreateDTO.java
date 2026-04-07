package com.taskhub.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TaskCreateDTO {
    private String title;
    private String description;
    private String projectId;
    private String assigneeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer duration;
    private Float estimatedHours;
    private String priority;
    private String status;
    private Integer isMilestone;
    private LocalDate milestoneDate;
    private String tags;
}
