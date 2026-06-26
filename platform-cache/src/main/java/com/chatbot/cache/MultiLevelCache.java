package com.chatbot.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * L1 Caffeine + L2 Redis 多级缓存，用于降低大模型重复调用成本。
 */
@Component
@RequiredArgsConstructor
public class MultiLevelCache {

    private final StringRedisTemplate redisTemplate;

    private final Cache<String, String> localCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();

    public String getOrLoad(String key, Duration ttl, Supplier<String> loader) {
        String cached = localCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }

        cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (!cached.isEmpty()) {
                localCache.put(key, cached);
            }
            return cached.isEmpty() ? null : cached;
        }

        String value = loader.get();
        if (value == null) {
            redisTemplate.opsForValue().set(key, "", Duration.ofMinutes(1));
            return null;
        }

        redisTemplate.opsForValue().set(key, value, ttl);
        localCache.put(key, value);
        return value;
    }

    public void evict(String key) {
        localCache.invalidate(key);
        redisTemplate.delete(key);
    }

    public void put(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
        localCache.put(key, value);
    }
}
