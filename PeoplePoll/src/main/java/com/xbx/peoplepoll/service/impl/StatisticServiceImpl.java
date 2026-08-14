package com.xbx.peoplepoll.service.impl;

import com.xbx.peoplepoll.mapper.*;
import com.xbx.peoplepoll.pojo.*;
import com.xbx.peoplepoll.service.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author X
 * @date 2026/5/10 11:18
 */



     /*
     前端需要
     ['timePeriod', '晚间', '凌晨', '午夜', '下午', '早晨', '上午'], // x轴
     ['官方媒体(蓝V)', 56.5, 82.1, 88.7, 70.1, 53.4, 85.1],
     ['个人黄V', 51.1, 51.4, 55.1, 53.3, 73.8, 68.7],
     ['个人金V', 40.1, 62.2, 69.5, 36.4, 45.2, 32.5],
     ['未认证用户', 25.2, 37.1, 41.2, 18, 33.9, 49.1]
     ['未认证用户', 25.2, 37.1, 41.2, 18, 33.9, 49.1]
      */

@Service
public class StatisticServiceImpl implements StatisticService {

    // 一天的流程：凌晨、早晨、上午、下午、晚间、午夜
    private static final List<String> TIME_ORDER = Arrays.asList(
            "凌晨", "早晨", "上午", "下午", "晚间", "午夜"
    );
    @Autowired
    private UserPostCountMapper userPostCountMapper;

    @Autowired
    private TimePeriodUserCountMapper timePeriodUserCountMapper;

    @Autowired
    private RegionHotMapper regionHotMapper;
    @Override
    public List<List<Object>> getTotalStatisticData() {
        // 1. 最终要返回的数据
        List<List<Object>> result = new ArrayList<>();
        //  构建第一行：表头
        List<Object> header = new ArrayList<>();
        header.add("timePeriod");
        header.addAll(TIME_ORDER);
        result.add(header);

        // 从数据库拿所有统计数据
        List<TimePeriodUser> allData = timePeriodUserCountMapper.selectAll();

        // 转成 MAP：key=时间段_用户类型 value=数量
        Map<String, Integer> dataMap = new HashMap<>();
        Set<String> userTypes = new LinkedHashSet<>();

        for (TimePeriodUser item : allData) {
            String key = item.getTimePeriod() + "_" + item.getUserAuthentication();
            dataMap.put(key, item.getCount());
            userTypes.add(item.getUserAuthentication());
        }

        //  按用户构建每一行
        for (String userType : userTypes) {
            List<Object> row = new ArrayList<>();
            row.add(userType); // 第一列：用户类型

            // 按照前端顺序，把6个时间段的值依次放进去
            for (String time : TIME_ORDER) {
                String key = time + "_" + userType;
                row.add(dataMap.getOrDefault(key, 0));
            }

            result.add(row);
        }

        return result;
    }

    @Override
    public List<UserPost> getUserStatisticData() {
        List<UserPost> userPost = userPostCountMapper.selectAll();
        return userPost;
    }

    // 地图热点数据
    @Override
    public List<RegionHot> getRegionHotData() {
        List<RegionHot> list = regionHotMapper.selectAll();
        if (list == null || list.isEmpty()) return list;

        // 1. 找最大最小值
        long max = list.stream().mapToLong(RegionHot::getCount).max().orElse(1);
        long min = list.stream().mapToLong(RegionHot::getCount).min().orElse(0);

        // 2. 归一化到 0~100，避免 max==min 时除以0
        long range = max == min ? 1 : max - min;

        list.forEach(r -> {
            double hot = (r.getCount() - min) * 100.0 / range;
            r.setHot((double) Math.round(hot * 100) / 100);
        });

        return list;
    }
}
