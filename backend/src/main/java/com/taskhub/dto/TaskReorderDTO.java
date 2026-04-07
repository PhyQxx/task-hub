package com.taskhub.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TaskReorderDTO {
    private String taskId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean autoReschedule;
}
