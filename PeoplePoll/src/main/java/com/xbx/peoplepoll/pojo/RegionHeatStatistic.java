package com.xbx.peoplepoll.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author X
 * @date 2026/5/20 9:31
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
/*
* 地区情感统计
* */
public class RegionHeatStatistic {
    private String region;
    private Double avgScore;
    private Integer count;
    private LocalDateTime createTime;
}
