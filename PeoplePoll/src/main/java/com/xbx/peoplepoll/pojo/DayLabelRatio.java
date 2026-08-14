package com.xbx.peoplepoll.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * @author X
 * @date 2026/5/20 16:23
 */
/*
* 舆情随日期变化表
* */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DayLabelRatio {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dt;
    private String label;
    private Integer value;
    private Integer total;
    private Double ratio;
}
