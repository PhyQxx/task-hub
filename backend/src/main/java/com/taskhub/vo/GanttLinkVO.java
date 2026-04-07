package com.taskhub.vo;

import lombok.Data;

@Data
public class GanttLinkVO {
    private String id;
    private String source;
    private String target;
    private String type;
}
