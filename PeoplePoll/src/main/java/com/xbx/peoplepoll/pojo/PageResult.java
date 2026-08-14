package com.xbx.peoplepoll.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author X
 * @date 2026/5/31 22:55
 */
/*
* 分页返回对象
* */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    // T是泛型，表示返回的数据类型
public class PageResult<T> {
    private Long total; // 总记录数
    private List<T> rows;// 当前页的数据
}
