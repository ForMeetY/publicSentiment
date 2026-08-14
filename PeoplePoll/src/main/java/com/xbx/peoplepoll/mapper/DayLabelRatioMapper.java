package com.xbx.peoplepoll.mapper;


import com.xbx.peoplepoll.pojo.DayLabelRatio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
@Mapper
public interface DayLabelRatioMapper {
    // 查询全部
    @Select("select * from day_label_ratio")
    List<DayLabelRatio> SelectAll();
}
