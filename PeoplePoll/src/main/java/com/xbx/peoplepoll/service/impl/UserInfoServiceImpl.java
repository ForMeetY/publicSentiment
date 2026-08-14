package com.xbx.peoplepoll.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xbx.peoplepoll.mapper.UserGmmProfileMapper;
import com.xbx.peoplepoll.pojo.AiAnalysisDTO;
import com.xbx.peoplepoll.pojo.PageResult;
import com.xbx.peoplepoll.pojo.UserGmmProfile;
import com.xbx.peoplepoll.service.UserInfoService;
import com.xbx.peoplepoll.utils.cache.SentimentDataCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author X
 * @date 2026/5/31 22:29
 */

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    private UserGmmProfileMapper userGmmProfileMapper;
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SentimentDataCache dataCache;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Override
    public PageResult<UserGmmProfile> getAllUserInfo(Integer page, Integer pageSize) {
        String cacheKey = String.format("weibo:user_list:page:%d:size:%d", page, pageSize);
        // 1. 尝试从缓存获取
        Object cachedData = dataCache.get(cacheKey);
        if (cachedData != null) {
            // 直接转换为 PageResult 对象
            return JSON.parseObject(cachedData.toString(), new TypeReference<PageResult<UserGmmProfile>>(){});
        }
        // 分页
        PageHelper.startPage(page, pageSize);
        List<UserGmmProfile> userGmmProfileList = userGmmProfileMapper.getAllUserInfo();
        // 对分页进行解析
        Page<UserGmmProfile> pageResult = (Page<UserGmmProfile>) userGmmProfileList;
        // 3. 构建明确的 PageResult 对象存入缓存
        PageResult<UserGmmProfile> result = new PageResult<>(pageResult.getTotal(), pageResult.getResult());
        dataCache.put(cacheKey, JSON.toJSONString(result), 10, TimeUnit.MINUTES);
        //最后返回分页的结果而不是单纯的数组
        return new PageResult<UserGmmProfile>(pageResult.getTotal(), pageResult.getResult());
    }

    @Override
    public UserGmmProfile getUserInfoById(String userID) {
        UserGmmProfile userInfo = userGmmProfileMapper.getUserInfoById(userID);
        return userInfo;
    }

    @Override
//    @Cacheable(value = "aiCache", key = "'weibo:ai:'+#userID")
    public AiAnalysisDTO getAiAnalysisResult(String userID) {
        UserGmmProfile user = userGmmProfileMapper.getUserInfoById(userID);
        String cacheKey = "weibo:ai:" + userID;
        // 获取 Redis 中的原始 JSON 字符串
        Object cachedObj = dataCache.get(cacheKey);

        if (cachedObj != null) {
            // 如果是 String，直接转；如果是 JSONObject，转成字符串再转 DTO
            String jsonStr = cachedObj.toString();
            if (cachedObj instanceof JSONObject) {
                jsonStr = ((JSONObject) cachedObj).toJSONString();
            }
            return JSON.parseObject(jsonStr, AiAnalysisDTO.class);
        }
        if (user == null) return new AiAnalysisDTO("数据缺失", "无法找到该用户画像", "无");
        double prob = 0.0;
        if (user.getClusterId() == 0) prob = user.getProb0();
        else if (user.getClusterId() == 1) prob = user.getProb1();
        else if (user.getClusterId() == 2) prob = user.getProb2();
        else if (user.getClusterId() == 3) prob = user.getProb3();
        String prompt = String.format(
                "你是一名社交舆情专家。请分析以下用户数据：\n" +
                        "- 用户ID: %s\n" +
                        "- 归属圈层ID: %d (0:生活号, 1:夜猫子, 2:大V, 3:宣泄群体)\n" +
                        "- 该用户属于该圈层的置信概率: %.2f%%\n" +
                        "- 情感标准差: %.2f\n" +
                        "- 舆论杠杆值: %.2f\n" +
                        "请结合以上特征进行画像分析。\n" +
                        "JSON格式要求: {\"userTag\": \"...\", \"behaviorAnalysis\": \"...\", \"operationSuggestion\": \"...\"}",
                user.getUserId(),
                user.getClusterId(),
                (prob * 100),
                user.getHourlySentimentStddev(),
                user.getSentimentLeverage()
        );
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-v4-pro");

        // 构建 messages 列表
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://api.deepseek.com/chat/completions", entity, String.class);

            JSONObject root = JSON.parseObject(response.getBody());
            String content = root.getJSONArray("choices").getJSONObject(0)
                    .getJSONObject("message").getString("content");


            content = content.replace("```json", "").replace("```", "").trim();

            if (content != null) {
                AiAnalysisDTO dto = JSON.parseObject(content, AiAnalysisDTO.class);
                // 缓存60分支
                dataCache.put(cacheKey, dto, 60, TimeUnit.MINUTES);
                return dto;
            }
            return JSON.parseObject(content, AiAnalysisDTO.class);

        } catch (Exception e) {
            e.printStackTrace();
            return new AiAnalysisDTO("分析失败", "AI接口请求异常: " + e.getMessage(), "请检查网络或API Key配置");
        }
    }
}