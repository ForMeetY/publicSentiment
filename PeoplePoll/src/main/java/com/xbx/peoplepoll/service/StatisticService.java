package com.xbx.peoplepoll.service;

import com.xbx.peoplepoll.pojo.RegionHot;
import com.xbx.peoplepoll.pojo.UserPost;

import java.util.List;

public interface StatisticService {

    public List<List<Object>> getTotalStatisticData();

    // 获取各个用户认证的发帖数
    List<UserPost> getUserStatisticData();

    // 请求地图热点数据
    List<RegionHot> getRegionHotData();
}
