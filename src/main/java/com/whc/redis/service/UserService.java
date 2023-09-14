package com.whc.redis.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whc.redis.controller.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author WuHaichao
 * @description
 * @email haichao0099@gmail.com
 * @date 2023/9/12 18:51
 */
public interface UserService extends IService<User> {

    List<User> findWithRedis();
}
