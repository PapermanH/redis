package com.whc.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author WuHaichao
 * @description
 * @email haichao0099@gmail.com
 * @date 2023/9/13 21:19
 */
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redisson() {
        // 创建配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.0.100:6379");
        // 根据配置创建redisson实例
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
