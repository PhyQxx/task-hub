package com.taskhub.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class WorkLogCreateDTO {
    private String taskId;
    private String userId;
    private LocalDate logDate;
    private String todayDone;
    private String tomorrowPlan;
    private String currentStatus;
    private String blockedReason;
    private Float hoursSpent;
}
