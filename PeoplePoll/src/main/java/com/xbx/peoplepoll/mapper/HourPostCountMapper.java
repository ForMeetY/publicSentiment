package com.xbx.peoplepoll.mapper;

import com.xbx.peoplepoll.pojo.HourPost;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author X
 * @date 2026/5/9 21:14
 */
@Mapper
public interface HourPostCountMapper {
// 查询全部
    List<HourPost> selectAll();
}
