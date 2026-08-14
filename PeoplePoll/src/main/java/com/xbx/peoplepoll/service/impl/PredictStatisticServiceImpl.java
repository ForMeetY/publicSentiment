package com.xbx.peoplepoll.service.impl;

import com.xbx.peoplepoll.mapper.*;
import com.xbx.peoplepoll.pojo.*;
import com.xbx.peoplepoll.service.PredictStatisticService;
import org.apache.ibatis.annotations.DeleteProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author X
 * @date 2026/5/19 22:38
 */

@Service
public class PredictStatisticServiceImpl implements PredictStatisticService {
    @Autowired
    private LabelCountMapper labelCountMapper;
    @Autowired
    private LabelCountByTimeMapper labelCountByTimeMapper;
    @Autowired
    private RegionHeatStatisticMapper regionHeatStatisticMapper;
    @Autowired
    private InteractionCountByLabelMapper interactionCountByLabelMapper;
    @Autowired
    private UserAuthLabelCountMapper userAuthLabelCountMapper;
    @Autowired
    private DayLabelRatioMapper dayLabelRatioMapper;
    @Override
    public List<LabelCount> getLabelCount() {
        List<LabelCount> labelCounts = labelCountMapper.selectAll();
        return labelCounts;
    }

    @Override
    public List<LabelCountByTime> getLabelCountByTime() {
        List<LabelCountByTime> labelCountByTimes = labelCountByTimeMapper.SelectAll();
        return labelCountByTimes;
    }


    @Override
    public List<RegionHeatStatistic> getRegionHeatStatistics() {
        List<RegionHeatStatistic> regionHeatStatistics = regionHeatStatisticMapper.selectAll();
        return regionHeatStatistics;
    }

    // 交互度和情绪的统计

    @Override
    public List<InteractionCountByLabel> getInteractionEmotion() {
        List<InteractionCountByLabel> interactionCountByLabels = interactionCountByLabelMapper.selectAll();
        return interactionCountByLabels;
    }

    // 用户认证情绪统计
    @Override
    public List<UserAuthLabelCount> getUserAuthEmotion() {
        List<UserAuthLabelCount> userAuthLabelCounts = userAuthLabelCountMapper.selectAll();
        return userAuthLabelCounts;
    }


    // 舆情随日期的变化趋势
    @Override
    public List<DayLabelRatio> getOpinionTrend() {
        List<DayLabelRatio> dayLabelRatios = dayLabelRatioMapper.SelectAll();
        return dayLabelRatios;
    }
}
