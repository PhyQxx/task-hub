package com.taskhub.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchTaskUpdateDTO {
    private List<String> taskIds;
    private String status;
    private String assigneeId;
    private String priority;
}
