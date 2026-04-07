package com.taskhub.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchScheduleDTO {
    private List<String> taskIds;
    private String projectId;
    private String mode;
}
