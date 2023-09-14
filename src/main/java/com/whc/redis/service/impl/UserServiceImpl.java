package com.whc.redis.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whc.redis.controller.entity.User;
import com.whc.redis.dao.UserMapper;
import com.whc.redis.service.UserService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author WuHaichao
 * @description
 * @email haichao0099@gmail.com
 * @date 2023/9/12 18:51
 * <p>
 * 1. 空结果缓存：解决缓存穿透
 * 2. 设置过期时间（加随机值）：解决缓存雪崩
 * 3. 加锁：解决缓存击穿
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 引入redis
     *
     * @return
     */
    @Override
    public List<User> findWithRedis() {

        // 1.加入缓存
        String usersJson = redisTemplate.opsForValue().get("usersJson");
        if (!StringUtils.hasLength(usersJson)) {
            System.out.println("=======缓存未命中=======");
            // 2.缓存中没有、查询数据库
            List<User> list = getUserListWithRedisson();

            return list;
        }

        System.out.println("=======缓存命中，直接返回=======");
        List<User> result = JSON.parseObject(usersJson, new TypeReference<List<User>>() {
        });
        return result;
    }

    /**
     * 加本地锁解决缓存击穿问题
     *
     * @return
     */
    public List<User> getUserListWithLocalLock() {

        // 只要是同一把锁，就能锁住需要这个锁的所有线程
        // 本地锁只能锁当前进程资源，在分布式情况下，想要锁住所有，必须使用分布式锁
        synchronized (this) {
            return getUsersFromDB();
        }
    }

    /**
     * redis分布式锁
     *
     * @return
     */
    public List<User> getUserListWithRedisLock() {

        String uuid = UUID.randomUUID().toString();
        // 1.占分布式锁，去redis占坑
//        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "111");
        // 加锁+设置过期时间原子性操作
        // 固定的value(111)可能会导致，如果当前业务逻辑较长，超过过期时间，锁被自动释放，其他线程会进来，等到删除锁的时候，删除的就是别人的锁了
//        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", "111",30,TimeUnit.MINUTES);
        // 指定value的值唯一，结合释放锁之前的判断，保证删除的是自己的锁
        // 如果业务事件超长，则需要考虑锁的续期，若不想续期，就讲过期时间设大一点
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 30, TimeUnit.MINUTES);

        if (lock) {
            // 加锁成功
//            // 设置过期时间,必须和加锁是同步的，原子的，下面这种如果在占到锁后，设置过期时间之前出现宕机，就会导致死锁
//            redisTemplate.expire("lock",30, TimeUnit.MINUTES);

            System.out.println("加锁成功");
            List<User> usersFromDB;
            try {
                usersFromDB = getUsersFromDB();
            } finally {
                // 释放锁
//            redisTemplate.delete("lock");

                // 以下方式，拿到锁和删除时中间如果锁过期，其他线程就会进来，再删除的时候 删除的就是别人的锁了 因此，删除锁必须保证原子性
//            String lockValue = redisTemplate.opsForValue().get("lock");
//            if (uuid.equals(lockValue)) {
//                redisTemplate.delete("lock");
//            }

                // 结合lua脚本 实现原子性删锁
                System.out.println("======开始删除锁====== : " + uuid);
                String script = "if redis.call('get',KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call('del',KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
                Long deleteLock = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                        Arrays.asList("lock"),
                        uuid);
                if (deleteLock.equals(1l)) {
                    System.out.println("锁lock: "+uuid+" 释放成功");
                }
            }

            return usersFromDB;

        } else {
            // 加锁失败。。。重试（自旋）
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("==========自旋尝试重新加锁==========");
            return getUserListWithRedisLock();
        }
    }

    /**
     * Redisson分布式锁
     *
     * @return
     */
    public List<User> getUserListWithRedisson() {

        // 1.获取一把锁，只要锁名一样，就是同一把锁
        RLock myLock = redissonClient.getLock("myLock");

        // 2.加锁 阻塞式等待
//        myLock.lock(); // 默认30秒过期，有看门狗模式自动续期

        // 若手动设置过期时间，时间一定要大于业务执行时间
        myLock.lock(10,TimeUnit.SECONDS);

        // 尝试加锁，最多等待100秒，上锁以后10秒自动解锁
//        boolean res = myLock.tryLock(100, 10, TimeUnit.SECONDS);

        List<User> users = null;
        try {
            System.out.println("加锁成功，执行业务："+Thread.currentThread().getId());
            users = getUsersFromDB();
            Thread.sleep(10000);
        }catch (Exception e){

        }finally {
            // 3.解锁
            System.out.println("释放锁："+Thread.currentThread().getId());
            myLock.unlock();
        }

        return users;
    }

    private List<User> getUsersFromDB() {
        // 得到锁之后，要去缓存中确认一次，如果没有再继续查询数据库
        String usersJson = redisTemplate.opsForValue().get("usersJson");
        if (StringUtils.hasLength(usersJson)) {
            System.out.println("拿到锁，从缓存中再次却认，有数据");
            List<User> result = JSON.parseObject(usersJson, new TypeReference<List<User>>() {
            });
            return result;
        }
        System.out.println("=======开始查询数据库=======");
        List<User> result = baseMapper.selectList(null);
        // 3.查到的数据再放入缓存,转为json字符串
        redisTemplate.opsForValue().set("usersJson", JSON.toJSONString(result));
        return result;
    }
}
