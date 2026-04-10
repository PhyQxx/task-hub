package com.taskhub.vo;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class GanttTaskVO {
    private String id;
    private String text;
    private LocalDate start_date;
    private Integer duration;
    private Float progress;
    private String parent;
    private String type;
    private Integer ignore_flag;
    private Integer combine_sign_flag;
    private LocalDate taskStartTime;
    private LocalDate taskEndTime;
    private Float taskDurationHours;
    private String status;
    private String priority;
    private String description;
    private String assignee_id;
    private String assignee_name;
    private Integer is_milestone;
    private LocalDate milestone_date;
    private List<GanttLinkVO> dependencies;
}
