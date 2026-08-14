package com.xbx.peoplepoll.service.impl;

import com.xbx.peoplepoll.utils.cache.SentimentDataCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 舆情大屏数据预计算定时清洗服务（9项指标全激活版）
 * @author X
 * @date 2026/5/29 17:17
 */
@Service
public class SentimentScheduleServiceImpl {

    @Autowired
    private SentimentDataCache dataCache;

    @Autowired
    private PredictStatisticServiceImpl predictStatisticService;

    @Autowired
    private StatisticServiceImpl statisticService;

    // 每 5 分钟执行一次
    @Scheduled(fixedRate = 1000 * 60 * 5)
    public void refreshSentimentCache() {
        try {
            System.out.println("读取9个接口数据...");

            Object labelCountData       = predictStatisticService.getLabelCount();
            Object labelCountByTimeData = predictStatisticService.getLabelCountByTime();
            Object regionHeatData       = predictStatisticService.getRegionHeatStatistics();
            Object interactionData      = predictStatisticService.getInteractionEmotion();
            Object userAuthEmotionData  = predictStatisticService.getUserAuthEmotion();
            Object opinionTrendData     = predictStatisticService.getOpinionTrend();
            Object totalStatisticData   = statisticService.getTotalStatisticData();
            Object userStatisticData    = statisticService.getUserStatisticData();
            Object regionHotData        = statisticService.getRegionHotData();

            // 统一非空校验
            boolean allValid = isNotEmpty(labelCountData)
                    && isNotEmpty(labelCountByTimeData)
                    && isNotEmpty(regionHeatData)
                    && isNotEmpty(interactionData)
                    && isNotEmpty(userAuthEmotionData)
                    && isNotEmpty(opinionTrendData)
                    && isNotEmpty(totalStatisticData)
                    && isNotEmpty(userStatisticData)
                    && isNotEmpty(regionHotData);

            if (!allValid) {
                System.err.println("存在空数据，本次不更新缓存");
                return;
            }

            // 更新缓存
            dataCache.put("labelCount", labelCountData);
            dataCache.put("labelCountByTime", labelCountByTimeData);
            dataCache.put("regionHeat", regionHeatData);
            dataCache.put("sentimentInteraction", interactionData);
            dataCache.put("sentimentUserAuth", userAuthEmotionData);
            dataCache.put("sentimentStack", opinionTrendData);
            dataCache.put("sentimentLinear", totalStatisticData);
            dataCache.put("userStatistic", userStatisticData);
            dataCache.put("sentimentMap", regionHotData);

            System.out.println("大屏9项数据缓存更新完成");

        } catch (Exception e) {
            System.err.println("全量数据更新异常，不更新缓存：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 非空判
     */
    private boolean isNotEmpty(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj instanceof Collection) {
            return !CollectionUtils.isEmpty((Collection<?>) obj);
        }

        if (obj instanceof Map) {
            return !((Map<?, ?>) obj).isEmpty();
        }

        if (obj.getClass().isArray()) {
            return !ObjectUtils.isEmpty(obj);
        }

        if (obj instanceof String) {
            return !((String) obj).trim().isEmpty();
        }

        return true;
    }
}