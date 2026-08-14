package com.xbx.peoplepoll.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author X
 * @date 2026/5/20 8:23
 */
/*
* 存储标签随时间的数量
* */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabelCountByTime {
    private String timePeriod;
    private String label;
    private Integer value;
    private LocalDateTime createTime;
}
