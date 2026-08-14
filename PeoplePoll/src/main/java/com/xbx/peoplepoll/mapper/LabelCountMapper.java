package com.xbx.peoplepoll.mapper;

import com.xbx.peoplepoll.pojo.LabelCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface LabelCountMapper {

    // 查询全部
    @Select("select * from label_count")
    List<LabelCount> selectAll();
}
