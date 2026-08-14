package com.xbx.peoplepoll.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author X
 * @date 2026/5/31 17:48
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserGmmProfile {
    private String userId;

    private String screenName;

    private Double hourlySentimentStddev;

    private Double nightPostRatio;

    private Long regionDiversityScore;

    private Double topRegionAvgScore;

    private Double sentimentLeverage;

    private Integer clusterId;

    private Double prob0;

    private Double prob1;

    private Double prob2;

    private Double prob3;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
