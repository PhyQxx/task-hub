package com.taskhub.vo;

import lombok.Data;
import java.util.List;

@Data
public class MemberLoadTrendVO {
    private String memberId;
    private List<WeekLoadVO> weeks;

    @Data
    public static class WeekLoadVO {
        private String week;
        private String weekLabel;
        private Integer loadPercent;
        private Integer taskCount;
        private String loadLevel;
    }
}
