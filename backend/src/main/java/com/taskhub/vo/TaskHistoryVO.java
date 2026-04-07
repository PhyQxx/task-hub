package com.taskhub.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskHistoryVO {
    private List<HistoryItemVO> history;

    @Data
    public static class HistoryItemVO {
        private String action;
        private String operator;
        private Object detail;
        private LocalDateTime createdAt;
    }
}
