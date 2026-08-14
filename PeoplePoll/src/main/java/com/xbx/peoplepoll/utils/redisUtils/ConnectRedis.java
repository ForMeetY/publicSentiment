package com.xbx.peoplepoll.utils.redisUtils;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import static com.xbx.peoplepoll.utils.redisUtils.RedisConstant.*;

/**
 * @author X
 * @date 2026/5/10 9:20
 */
// 连接redis 集群
public class ConnectRedis {


    public static JedisCluster ConnectRedisCluster() {
        // 创建节点
        Set<HostAndPort> nodes = new HashSet<>();
        // 超时
        int timeout = 10000; // 连接超时
        int readTimeout = 10000; // 读取超时
        int maxAttempts = 5; // 最大重试次数
        // 配置连接池
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(10);
        poolConfig.setMinIdle(1);
        poolConfig.setMaxWaitMillis(1000);
        nodes.add(new HostAndPort(
                REDIS_HOST1,
                REDIS_PORT1));
        nodes.add(new HostAndPort(
                REDIS_HOST2,
                REDIS_PORT2));
        nodes.add(new HostAndPort(
                REDIS_HOST3,
                REDIS_PORT3));
        // 创建连接
        JedisCluster jedisCluster = new JedisCluster(
                nodes,
                timeout,
                readTimeout,
                maxAttempts,
                REDIS_PASSWORD,
                poolConfig
        );
        return jedisCluster;
    }

    public static Jedis ConnectRedis(String host, int port, String password) {
        Jedis jedis = new Jedis(host, port);
        jedis.auth(password);
        return jedis;
    }

    public static Jedis ConnectRedis(String host, int port) {
        Jedis jedis = new Jedis(host, port);
        return jedis;
    }

    // 连接mysql
    public static Connection ConnectMysql() {
        try {
            // 1. 注册驱动（MySQL 8+ 自动注册，不用写）
            // 2. 连接信息
            String url = "jdbc:mysql://master1:3306/weibo?useSSL=false&serverTimezone=Asia/Shanghai";
            String user = "root";
            String password = "1234";
            // 3. 获取连接并返回
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("MySQL连接失败");
        }
    }


}
