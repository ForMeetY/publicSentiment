package com.xbx.peoplepoll.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author X
 * @date 2026/4/23 22:17
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMem {
    private Integer id;
    private String title;
    private String description; //'项目描述、功能简介
    private String path; // 路由
    private String domain;// '项目域名', 备用
    private String icon;
    private Integer sort; // '排序权重 数字越小越靠前',
    private Integer isShow; // '是否显示 0-否 1-是',
    private String remark; // 备用
    private String img;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
