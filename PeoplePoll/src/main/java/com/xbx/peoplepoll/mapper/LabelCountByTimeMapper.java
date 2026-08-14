package com.xbx.peoplepoll.mapper;


import com.xbx.peoplepoll.pojo.LabelCountByTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
@Mapper
public interface LabelCountByTimeMapper {

    @Select("select * from label_count_by_time")
    List<LabelCountByTime> SelectAll();
}
