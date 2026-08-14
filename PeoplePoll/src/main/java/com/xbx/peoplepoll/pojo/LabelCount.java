package com.xbx.peoplepoll.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author X
 * @date 2026/5/19 22:33
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LabelCount {
    private String label;
    private Integer value;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}
