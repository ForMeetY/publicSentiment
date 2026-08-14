package com.xbx.peoplepoll.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author X
 * @date 2026/5/11 14:42
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimePeriodUser {
    private String timePeriod;
    private String userAuthentication;
    private Integer count;
    private LocalDateTime createTime;
}
