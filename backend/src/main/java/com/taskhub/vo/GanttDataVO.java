package com.taskhub.vo;

import lombok.Data;
import java.util.List;

@Data
public class GanttDataVO {
    private List<GanttTaskVO> data;
    private List<GanttLinkVO> links;
}
