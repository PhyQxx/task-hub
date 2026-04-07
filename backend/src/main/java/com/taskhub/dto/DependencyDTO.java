package com.taskhub.dto;

import lombok.Data;

@Data
public class DependencyDTO {
    private String taskId;
    private String dependsOn;
    private String dependencyType;
}
