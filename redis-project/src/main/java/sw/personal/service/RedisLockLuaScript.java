package sw.personal.service;


import java.util.Collections;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/*
 *
 */

/**
 * @Author wenrenhao
 * @Date 2020-11-29 00:32
 * @Version 1.0
 * @Context  使用redis的脚本实现分布式锁, 利用lua脚本原子性，返回锁资源 完整的把分布工锁机制解决
 */

@Service
public class RedisLockLuaScript {

    @Autowired
    RedisTemplate<String, String> redisTemplate;


    private static final Long SUCCESS = 1L;
    public static final String LOCK_PRE_KEY = "nnl_";

    // 加锁脚本
    private static final String SCRIPT_LOCK = "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then redis.call('pexpire', KEYS[1], ARGV[2]) return 1 else return 0 end";
    // 解锁脚本
    private static final String SCRIPT_UNLOCK = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";


    public boolean getLock(String key, String requestId) {
        long start = System.currentTimeMillis();
        StringBuilder lockScript = new StringBuilder();
        lockScript.append("local ok = redis.call('setnx', '")
                .append(key)
                .append("', ARGV[1]);if ok == 1 then redis.call('expire', '")
                .append(key)
                .append("', 5001) end; return ok");
        while (true) {
            //返回参数不一样 按照类型分，主要看脚本返回什么，商品秒杀返回的是字符串，这里返回数字 5，
            DefaultRedisScript<Long> longDefaultRedisScript = new DefaultRedisScript<>(lockScript.toString(), Long.class);
            Long result = redisTemplate.execute(longDefaultRedisScript,
                    Collections.singletonList(key),// KEYS[1]
                    requestId // ARGV[1]
                    //  String.valueOf(expireTimeMilliseconds) // ARGV[2]
            );
            Long ret = (Long) result;
            if (ret != null && ret == 1){
                return true;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() - start > 15 * 1000) {
                redisTemplate.delete(LOCK_PRE_KEY + key);
            }
        }
    }

    /**
     * 如果一直依赖TTL来释放锁，效率会很低。Redis的SET操作文档就提供了一个释放锁的脚本：
     * 释放分布式锁
     *
     * @param lockKey       锁
     * @param requestId     请求标识
     * @return 返回true表示释放锁成功
     */
    public boolean releaseLock(String lockKey, String requestId) {

        DefaultRedisScript<Long> longDefaultRedisScript = new DefaultRedisScript<>(SCRIPT_UNLOCK, Long.class);
        Long result = redisTemplate.execute(longDefaultRedisScript,
                Collections.singletonList(lockKey),// KEYS[1]
                requestId // ARGV[1]  删除KEY的标识
                //  String.valueOf(expireTimeMilliseconds) // ARGV[2]   过期时间
        );
        return SUCCESS.equals(result);
    }

}