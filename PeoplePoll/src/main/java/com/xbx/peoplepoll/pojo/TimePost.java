package com.xbx.peoplepoll.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author X
 * @date 2026/5/9 21:21
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimePost {
    private String timePeriod;
    private Integer count;
    private String createTime;
}
