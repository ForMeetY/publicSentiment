package com.xbx.peoplepoll.service;

import com.xbx.peoplepoll.pojo.*;
import org.springframework.stereotype.Service;
import java.util.List;

public interface PredictStatisticService {

    List<LabelCount> getLabelCount();

    // 获取label和时间段上午下午....的统计
    List<LabelCountByTime> getLabelCountByTime();

    List<RegionHeatStatistic> getRegionHeatStatistics();

    // 交互度和情绪统计
    List<InteractionCountByLabel> getInteractionEmotion();

    // 用户认证情绪的统计
    List<UserAuthLabelCount> getUserAuthEmotion();

    // 舆情随日期的变化趋势
    List<DayLabelRatio> getOpinionTrend();
}
