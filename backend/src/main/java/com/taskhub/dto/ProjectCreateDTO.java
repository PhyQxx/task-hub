package com.taskhub.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ProjectCreateDTO {
    private String name;
    private String description;
    private String ownerId;
    private LocalDate startDate;
    private LocalDate endDate;
}
