package com.xbx.peoplepoll.mapper;

import com.xbx.peoplepoll.pojo.DayPost;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author X
 * @date 2026/5/9 21:13
 */
@Mapper
public interface DayPostCountMapper {
    // 查询全部
    List<DayPost> selectAll();
}
