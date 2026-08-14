package com.xbx.peoplepoll.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author X
 * @date 2026/5/13 22:06
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegionHot {
           private String region;
           private Integer count;
           private Double hot;
           @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
           private LocalDateTime create_time;
}
