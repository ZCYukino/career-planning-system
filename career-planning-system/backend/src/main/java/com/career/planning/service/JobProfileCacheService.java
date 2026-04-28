package com.career.planning.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

@Service
public class JobProfileCacheService {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private static final long CACHE_EXPIRE_MS = 3600000; // 1小时过期

    /** 已完成/失败的任务状态过期时间（5分钟） */
    private static final long TASK_STATUS_EXPIRE_MS = 5 * 60 * 1000;

    /** 异步任务状态（岗位名 -> 状态） */
    private final ConcurrentHashMap<String, AsyncTaskStatus> taskStatus = new ConcurrentHashMap<>();

    public static class CacheEntry {
        public String profile;
        public long timestamp;

        public CacheEntry(String profile) {
            this.profile = profile;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRE_MS;
        }
    }

    public static class AsyncTaskStatus {
        public String status; // PROCESSING, COMPLETED, FAILED
        public int progress;
        public String stepName;
        public String errorMessage;
        /** 状态最终确定的时间戳，用于过期清理 */
        public long completedAt;

        public AsyncTaskStatus() {
            this.status = "PROCESSING";
            this.progress = 0;
            this.stepName = "正在初始化...";
            this.completedAt = 0;
        }
    }

    public String get(String jobName) {
        CacheEntry entry = cache.get(normalizeJobName(jobName));
        if (entry != null && !entry.isExpired()) {
            return entry.profile;
        }
        return null;
    }

    public void put(String jobName, String profile) {
        cache.put(normalizeJobName(jobName), new CacheEntry(profile));
    }

    public void clear() {
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    private String normalizeJobName(String jobName) {
        if (jobName == null) return "";
        return jobName.trim().toLowerCase();
    }

    // ========== 异步任务状态管理 ==========

    public void startTask(String jobName) {
        taskStatus.put(normalizeJobName(jobName), new AsyncTaskStatus());
    }

    public void updateTaskProgress(String jobName, int progress, String stepName) {
        AsyncTaskStatus status = taskStatus.get(normalizeJobName(jobName));
        if (status != null) {
            status.progress = progress;
            status.stepName = stepName;
        }
    }

    public void completeTask(String jobName, String profile) {
        AsyncTaskStatus status = taskStatus.get(normalizeJobName(jobName));
        if (status != null) {
            status.status = "COMPLETED";
            status.progress = 100;
            status.stepName = "画像生成完成";
            status.completedAt = System.currentTimeMillis();
        }
        put(jobName, profile);
    }

    public void failTask(String jobName, String errorMessage) {
        AsyncTaskStatus status = taskStatus.get(normalizeJobName(jobName));
        if (status != null) {
            status.status = "FAILED";
            status.progress = 0;
            status.stepName = "生成失败";
            status.errorMessage = errorMessage;
            status.completedAt = System.currentTimeMillis();
        }
    }

    public AsyncTaskStatus getTaskStatus(String jobName) {
        return taskStatus.get(normalizeJobName(jobName));
    }

    public boolean isTaskProcessing(String jobName) {
        AsyncTaskStatus status = taskStatus.get(normalizeJobName(jobName));
        return status != null && "PROCESSING".equals(status.status);
    }

    /**
     * Bean销毁时清理所有缓存，防止内存泄漏
     */
    @PreDestroy
    public void cleanup() {
        cache.clear();
        taskStatus.clear();
    }

    /**
     * 清理已过期和已完成/失败的 taskStatus 条目，防止内存无限增长
     * 在每次 startTask 前调用
     */
    public void evictStaleTaskStatus() {
        long now = System.currentTimeMillis();
        // 清理已完成/失败超过5分钟的任务状态
        taskStatus.entrySet().removeIf(entry -> {
            AsyncTaskStatus status = entry.getValue();
            return status.completedAt > 0 && (now - status.completedAt) > TASK_STATUS_EXPIRE_MS;
        });
        // 清理过期的缓存条目
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
