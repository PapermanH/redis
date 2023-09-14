package com.whc.redis.controller;

import com.whc.redis.controller.entity.User;
import com.whc.redis.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author WuHaichao
 * @description
 * @email haichao0099@gmail.com
 * @date 2023/9/12 18:36
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/find")
    public List<User> find() {

        List<User> users = userService.findWithRedis();

        return users;
    }
}
