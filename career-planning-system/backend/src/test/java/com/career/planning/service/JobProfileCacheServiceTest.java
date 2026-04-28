package com.career.planning.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JobProfileCacheService 测试
 * 验证缓存管理、任务状态管理、过期清理等逻辑
 */
class JobProfileCacheServiceTest {

    private JobProfileCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new JobProfileCacheService();
    }

    // ========== 缓存基本操作 ==========

    @Test
    @DisplayName("put 和 get 正常工作")
    void testPutAndGet() {
        cacheService.put("Java开发工程师", "{\"job_title\":\"Java开发工程师\"}");
        String result = cacheService.get("Java开发工程师");
        assertNotNull(result);
        assertTrue(result.contains("Java开发工程师"));
    }

    @Test
    @DisplayName("get 未缓存的 key 应返回 null")
    void testGetNonExistent() {
        assertNull(cacheService.get("不存在的岗位"));
    }

    @Test
    @DisplayName("缓存 key 应忽略大小写")
    void testCacheKeyNormalization() {
        cacheService.put("Java开发工程师", "profile1");
        assertEquals("profile1", cacheService.get("java开发工程师"));
        // trim 后的空格也会被忽略
        assertEquals("profile1", cacheService.get(" Java开发工程师 "));
    }

    @Test
    @DisplayName("put 会覆盖已有缓存")
    void testPutOverwrites() {
        cacheService.put("前端工程师", "v1");
        cacheService.put("前端工程师", "v2");
        assertEquals("v2", cacheService.get("前端工程师"));
    }

    @Test
    @DisplayName("clear 清空所有缓存")
    void testClear() {
        cacheService.put("岗位A", "profileA");
        cacheService.put("岗位B", "profileB");
        assertEquals(2, cacheService.size());
        cacheService.clear();
        assertEquals(0, cacheService.size());
        assertNull(cacheService.get("岗位A"));
    }

    // ========== 异步任务状态管理 ==========

    @Test
    @DisplayName("startTask 和 getTaskStatus 正常工作")
    void testStartTaskAndGetStatus() {
        cacheService.startTask("Python工程师");
        JobProfileCacheService.AsyncTaskStatus status = cacheService.getTaskStatus("Python工程师");
        assertNotNull(status);
        assertEquals("PROCESSING", status.status);
        assertEquals(0, status.progress);
    }

    @Test
    @DisplayName("updateTaskProgress 正确更新进度")
    void testUpdateTaskProgress() {
        cacheService.startTask("数据分析");
        cacheService.updateTaskProgress("数据分析", 50, "正在生成...");
        JobProfileCacheService.AsyncTaskStatus status = cacheService.getTaskStatus("数据分析");
        assertNotNull(status);
        assertEquals(50, status.progress);
        assertEquals("正在生成...", status.stepName);
    }

    @Test
    @DisplayName("completeTask 设置完成状态并缓存结果")
    void testCompleteTask() {
        cacheService.startTask("产品经理");
        cacheService.completeTask("产品经理", "{\"completed\":true}");
        
        JobProfileCacheService.AsyncTaskStatus status = cacheService.getTaskStatus("产品经理");
        assertNotNull(status);
        assertEquals("COMPLETED", status.status);
        assertEquals(100, status.progress);
        assertTrue(status.completedAt > 0, "completedAt 应被记录");
        
        // 同时应缓存结果
        assertEquals("{\"completed\":true}", cacheService.get("产品经理"));
    }

    @Test
    @DisplayName("failTask 设置失败状态")
    void testFailTask() {
        cacheService.startTask("UI设计师");
        cacheService.failTask("UI设计师", "AI服务超时");
        
        JobProfileCacheService.AsyncTaskStatus status = cacheService.getTaskStatus("UI设计师");
        assertNotNull(status);
        assertEquals("FAILED", status.status);
        assertEquals(0, status.progress);
        assertEquals("AI服务超时", status.errorMessage);
        assertTrue(status.completedAt > 0, "completedAt 应被记录");
    }

    @Test
    @DisplayName("isTaskProcessing 正确判断处理状态")
    void testIsTaskProcessing() {
        assertFalse(cacheService.isTaskProcessing("测试工程师"));
        
        cacheService.startTask("测试工程师");
        assertTrue(cacheService.isTaskProcessing("测试工程师"));
        
        cacheService.completeTask("测试工程师", "result");
        assertFalse(cacheService.isTaskProcessing("测试工程师"));
    }

    // ========== 过期清理（内存泄漏修复验证） ==========

    @Test
    @DisplayName("evictStaleTaskStatus 清理已完成超过5分钟的任务状态")
    void testEvictStaleCompletedTaskStatus() {
        cacheService.startTask("旧任务");
        cacheService.completeTask("旧任务", "旧结果");
        
        // 手动修改 completedAt 为 6 分钟前
        JobProfileCacheService.AsyncTaskStatus status = cacheService.getTaskStatus("旧任务");
        status.completedAt = System.currentTimeMillis() - 6 * 60 * 1000;
        
        cacheService.evictStaleTaskStatus();
        
        assertNull(cacheService.getTaskStatus("旧任务"), "过期的任务状态应被清理");
        // 注意：缓存条目有独立的1小时过期机制，不会因为任务状态过期而被清理
    }

    @Test
    @DisplayName("evictStaleTaskStatus 不清理正在处理的任务")
    void testEvictStaleDoesNotAffectProcessingTasks() {
        cacheService.startTask("进行中任务");
        
        cacheService.evictStaleTaskStatus();
        
        assertNotNull(cacheService.getTaskStatus("进行中任务"), "正在处理的任务不应被清理");
        assertTrue(cacheService.isTaskProcessing("进行中任务"));
    }

    @Test
    @DisplayName("evictStaleTaskStatus 不清理刚完成的任务")
    void testEvictStaleDoesNotAffectRecentCompleted() {
        cacheService.startTask("新完成任务");
        cacheService.completeTask("新完成任务", "新结果");
        
        cacheService.evictStaleTaskStatus();
        
        JobProfileCacheService.AsyncTaskStatus status = cacheService.getTaskStatus("新完成任务");
        assertNotNull(status, "刚完成的任务状态不应被清理");
    }

    @Test
    @DisplayName("cleanup 清空所有数据")
    void testCleanup() {
        cacheService.put("岗位1", "结果1");
        cacheService.startTask("岗位2");
        
        cacheService.cleanup();
        
        assertEquals(0, cacheService.size());
        assertNull(cacheService.getTaskStatus("岗位2"));
    }
}
