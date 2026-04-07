package com.taskhub.vo;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ReorderResultVO {
    private List<RescheduledTaskVO> rescheduled;
    private Integer cascaded;

    @Data
    public static class RescheduledTaskVO {
        private String taskId;
        private LocalDate oldEndDate;
        private LocalDate newStartDate;
    }
}
