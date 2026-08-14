package com.xbx.peoplepoll.utils.cache;

import com.alibaba.fastjson.JSON; // 换成 Fastjson
import com.xbx.peoplepoll.pojo.AiAnalysisDTO;
import com.xbx.peoplepoll.utils.redisUtils.ConnectRedis;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisCluster;

import java.util.concurrent.TimeUnit;

/**
 * 舆情大屏数据 Redis 集群缓存柜
 * @author X
 * @date 2026/5/29 17:15
 */
@Component
public class SentimentDataCache implements InitializingBean {

    private JedisCluster jedisCluster;

    @Override
    public void afterPropertiesSet() throws Exception {
        this.jedisCluster = ConnectRedis.ConnectRedisCluster();
    }

    public void put(String key, Object value, long time, TimeUnit unit) {
        jedisCluster.setex(key, (int) unit.toSeconds(time), JSON.toJSONString(value));
    }


    public void put(String key, Object value) {
        try {
            if (value == null) return;
            // 直接转成标准的 JSON 字符串
            String jsonStr = JSON.toJSONString(value);
            jedisCluster.setex(key, 36000, jsonStr);
        } catch (Exception e) {
            System.err.println("写入失败Key: " + key + "，原因: " + e.getMessage());
            throw new RuntimeException("Redis集群写入失败", e);
        }
    }

    // 取数据
    public Object get(String key) {
        try {
            String jsonStr = jedisCluster.get(key);
            if (jsonStr == null) return null;
            // 使用 Fastjson 解析还原
            return JSON.parse(jsonStr);
        } catch (Exception e) {
            System.err.println("Redis集群读取失败Key: " + key + "，原因: " + e.getMessage());
            return null;
        }
    }


}