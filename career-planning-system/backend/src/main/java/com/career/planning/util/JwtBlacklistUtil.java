package com.career.planning.util;

import com.career.planning.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * JWT Token 黑名单工具类
 *
 * 实现原理：JWT 本身无法主动失效（ stateless ），
 * 通过在黑名单中记录已失效的 Token，实现强制登出效果。
 *
 * 支持两种模式：
 * 1. Redis 模式（优先）：Token 存入 Redis，过期自动清理
 * 2. 内存模式（兜底）：Token 存入 ConcurrentHashMap，启动定时任务清理过期项
 */
@Component
public class JwtBlacklistUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtBlacklistUtil.class);
    private static final String REDIS_PREFIX = "jwt:blacklist:";
    private static final String USERNAME_PREFIX = "jwt:username:";

    private boolean redisEnabled = false;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // 内存模式兜底容器（key = token, value = 过期时间戳 ms）
    private final ConcurrentHashMap<String, Long> memoryBlacklist = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (redisTemplate != null) {
            try {
                // 测试 Redis 连接
                redisTemplate.opsForValue().get("__ping__");
                redisEnabled = true;
                log.info("✅ JWT 黑名单使用 Redis 模式");
            } catch (Exception e) {
                redisEnabled = false;
                log.warn("⚠️ Redis 连接失败，降级为内存模式: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ 未配置 StringRedisTemplate，降级为内存模式");
        }
    }

    /**
     * 将 Token 加入黑名单
     * 存储时长 = Token 剩余有效期，确保黑名单随 Token 过期自动失效
     *
     * @param token JWT Token
     */
    public void addToBlacklist(String token) {
        if (token == null || token.isEmpty()) return;

        if (redisEnabled) {
            addToBlacklistRedis(token);
        } else {
            addToBlacklistMemory(token);
        }
    }

    /**
     * 将 Token 加入黑名单，并记录其归属的 username（用于同用户多端互斥）
     *
     * @param token    JWT Token
     * @param username 用户名
     */
    public void addToBlacklistWithUsername(String token, String username) {
        if (token == null || token.isEmpty()) return;

        long ttlMs = jwtUtil.getRemainingMs(token);
        if (ttlMs <= 0) return; // Token 已过期，无需加入黑名单

        if (redisEnabled) {
            redisTemplate.opsForValue().set(REDIS_PREFIX + token, username, ttlMs, TimeUnit.MILLISECONDS);
            redisTemplate.opsForValue().set(USERNAME_PREFIX + username, token, ttlMs, TimeUnit.MILLISECONDS);
        } else {
            memoryBlacklist.put(token, System.currentTimeMillis() + ttlMs);
        }
    }

    /**
     * 校验 Token 是否在黑名单中
     *
     * @param token JWT Token
     * @return true = 在黑名单（已失效），false = 不在黑名单（有效）
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isEmpty()) return false;

        if (redisEnabled) {
            return Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_PREFIX + token));
        } else {
            Long expiry = memoryBlacklist.get(token);
            if (expiry == null) return false;
            if (expiry <= System.currentTimeMillis()) {
                memoryBlacklist.remove(token);
                return false;
            }
            return true;
        }
    }

    // ==================== Redis 模式 ====================

    private void addToBlacklistRedis(String token) {
        long ttlMs = jwtUtil.getRemainingMs(token);
        if (ttlMs <= 0) return;
        redisTemplate.opsForValue().set(REDIS_PREFIX + token, "1", ttlMs, TimeUnit.MILLISECONDS);
    }

    // ==================== 内存模式（兜底） ====================

    private void addToBlacklistMemory(String token) {
        long ttlMs = jwtUtil.getRemainingMs(token);
        if (ttlMs <= 0) return;
        memoryBlacklist.put(token, System.currentTimeMillis() + ttlMs);
    }

    /**
     * 定时清理过期黑名单项（每 10 分钟执行一次）
     * 仅在内存模式下需要；Redis 由 TTL 自动清理
     */
    @Scheduled(fixedRate = 600000)
    public void cleanupExpiredTokens() {
        if (redisEnabled) return;

        // 使用 removeIf 替代遍历删除，避免 ConcurrentModificationException
        memoryBlacklist.entrySet().removeIf(entry ->
                entry.getValue() != null && entry.getValue() <= System.currentTimeMillis());

        log.debug("内存黑名单清理完成，当前黑名单大小: {}", memoryBlacklist.size());
    }

    /**
     * 获取内存黑名单大小（仅用于监控）
     */
    public int getMemoryBlacklistSize() {
        return memoryBlacklist.size();
    }
}
