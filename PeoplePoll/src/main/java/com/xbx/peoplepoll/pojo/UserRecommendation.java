package com.xbx.peoplepoll.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author X
 * @date 2026/6/5 10:06
 */

/*
* 推荐类
*
* */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRecommendation {
        private Long id;
        private String userId;
        private String weiboId;
        private String bid;
        private String screenName;
        private String text;
        private String topicLabel;
        private String location;
        private Double score;
        private String recallReason;
        private LocalDateTime createTime;

}
