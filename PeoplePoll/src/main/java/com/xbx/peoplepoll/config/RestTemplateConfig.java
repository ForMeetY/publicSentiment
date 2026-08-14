package com.xbx.peoplepoll.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author X
 * @date 2026/6/3 9:23
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(500000);   // 连接超时 5秒
        factory.setReadTimeout(600000);     // 读取超时 30秒
        return new RestTemplate(factory);
    }
}
