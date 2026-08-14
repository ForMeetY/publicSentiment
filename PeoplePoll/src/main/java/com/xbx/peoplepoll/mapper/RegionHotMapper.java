package com.xbx.peoplepoll.mapper;

import java.util.List;


import com.xbx.peoplepoll.pojo.RegionHot;
import org.apache.ibatis.annotations.Mapper;




/**
 * @author X
 * @date 2026/5/13 22:23
 */
@Mapper
public interface RegionHotMapper {
    //获取全部
    List<RegionHot> selectAll();

}
