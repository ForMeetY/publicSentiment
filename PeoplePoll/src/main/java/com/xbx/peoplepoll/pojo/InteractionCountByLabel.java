package com.xbx.peoplepoll.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author X
 * @date 2026/5/20 10:45
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InteractionCountByLabel {
    private String label;
    private Double avgRepost;
    private Double avgComment;
    private Double avgAttitude;
    private LocalDateTime createTime;
}
