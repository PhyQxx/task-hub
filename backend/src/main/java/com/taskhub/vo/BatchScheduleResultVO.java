package com.taskhub.vo;

import lombok.Data;
import java.util.List;

@Data
public class BatchScheduleResultVO {
    private List<ScheduleItemVO> results;

    @Data
    public static class ScheduleItemVO {
        private String taskId;
        private String assigneeId;
        private String nickname;
        private Integer score;
        private String reason;
    }
}
