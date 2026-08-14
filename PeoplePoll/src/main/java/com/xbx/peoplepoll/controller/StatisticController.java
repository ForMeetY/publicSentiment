package com.xbx.peoplepoll.controller;


import com.xbx.peoplepoll.pojo.*;
import com.xbx.peoplepoll.service.StatisticService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xbx.peoplepoll.utils.cache.SentimentDataCache;
import java.util.List;


/**
 * @author X
 * @date 2026/5/9 21:10
 */
@Slf4j
@RestController
public class StatisticController {
    @Autowired
    private StatisticService statisticService;
    @Autowired
    private SentimentDataCache dataCache; // 注入内存缓存桶
    // 获取统计数据
    @GetMapping("/statistics")
    public Result getStatisticData(){
        Object cachedData = dataCache.get("sentimentLinear");
        if (cachedData != null) {
            return Result.success(cachedData);
        }
        List<List<Object>> data = statisticService.getTotalStatisticData();
        return Result.success(data);
    }

    @GetMapping("/statistics/user")
    public Result getUserStatisticData(){
        Object cachedData = dataCache.get("userStatistic");
        if (cachedData != null) {
            return Result.success(cachedData);
        }
        log.info("走数据库查询 userStatistics");
        List<UserPost> data = statisticService.getUserStatisticData();
        return Result.success(data);
    }

    // 请求地图热点数据
    @GetMapping("/statistics/region")
    public Result getRegionHotData(){
        Object cachedData = dataCache.get("sentimentMap");
        if (cachedData != null) {
            return Result.success(cachedData);
        }
        log.info("缓存未命中走数据库查询 regionHot地图");
        List<RegionHot> data = statisticService.getRegionHotData();
        return Result.success(data);
    }

}
