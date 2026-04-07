package com.taskhub.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class MilestoneCreateDTO {
    private String projectId;
    private String name;
    private LocalDate targetDate;
    private String description;
    private String color;
}
