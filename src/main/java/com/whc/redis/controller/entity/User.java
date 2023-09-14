package com.whc.redis.controller.entity;

import lombok.Data;

/**
 * @author WuHaichao
 * @description
 * @email haichao0099@gmail.com
 * @date 2023/9/12 18:39
 */
@Data
public class User {

    private Integer id;
    private Integer deviceId;
    private String gender;
    private Integer age;
    private String university;
    private Float gpa;
    private Integer questionCnt;
    private Integer answerCnt;
}
