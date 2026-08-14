package com.xbx.peoplepoll;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.xbx.peoplepoll.mapper")
@EnableScheduling
@EnableCaching
public class PeoplePollApplication {
    public static void main(String[] args) {
        SpringApplication.run(PeoplePollApplication.class, args);
    }

}
