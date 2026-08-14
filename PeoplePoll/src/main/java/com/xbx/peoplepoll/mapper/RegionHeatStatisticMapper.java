package com.xbx.peoplepoll.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.xbx.peoplepoll.pojo.RegionHeatStatistic;
import java.util.List;
@Mapper
public interface RegionHeatStatisticMapper {

    // 查询全部
    @Select("select * from region_heat_statistic")
    List<RegionHeatStatistic> selectAll();
}
