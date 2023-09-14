package com.whc.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 整合redis
 *      1.引入data-redis-starter
 *      2.配置redis的host等信息
 *      3.使用springboot自动配置好的StringRedisTemplate来操作redis
 * 整合redisson
 *      1.引入redisson依赖
 *
 */

@SpringBootApplication
public class RedisApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisApplication.class, args);
    }

}
