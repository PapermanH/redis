package com.whc.redis.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.whc.redis.controller.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author WuHaichao
 * @description
 * @email haichao0099@gmail.com
 * @date 2023/9/12 18:55
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
