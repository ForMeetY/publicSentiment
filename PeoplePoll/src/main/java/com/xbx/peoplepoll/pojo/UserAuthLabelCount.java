package com.xbx.peoplepoll.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author X
 * @date 2026/5/20 11:46
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAuthLabelCount {
    private String userAuthentication;
    private String label;
    private Integer value;
    private LocalDateTime createTime;
}
