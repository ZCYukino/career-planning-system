package com.career.planning.controller;

import com.career.planning.common.BusinessException;
import com.career.planning.common.CommonResult;
import com.career.planning.entity.JobBasicInfo;
import com.career.planning.service.AIService;
import com.career.planning.service.JobBasicInfoService;
import com.career.planning.service.JobProfileCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/job")
public class JobBasicInfoController {

    private static final Logger log = LoggerFactory.getLogger(JobBasicInfoController.class);

    @Autowired
    private JobBasicInfoService jobBasicInfoService;

    @Autowired
    private JobProfileCacheService cacheService;

    @Autowired
    private AIService aiService;

    @Autowired
    private TaskExecutor taskExecutor;

    // 获取核心岗位列表 (聚合后)
    @GetMapping("/analysis/summary")
    public CommonResult<List<Map<String, Object>>> getTopJobs() {
        // 严格按赛题口径：展示15个核心岗位
        return CommonResult.success(jobBasicInfoService.getTopJobs(15));
    }

    // 创建聚合画像生成任务（异步）
    @PostMapping("/analysis/profile/create")
    public CommonResult<Map<String, Object>> createAggregatedProfileTask(@RequestParam String jobName) {
        if (jobName == null || jobName.trim().isEmpty()) {
            return CommonResult.failed("岗位名称不能为空");
        }
        try {
            // 检查缓存
            String cached = cacheService.get(jobName);
            if (cached != null) {
                return CommonResult.success(Map.of(
                        "jobName", jobName,
                        "status", "COMPLETED",
                        "progress", 100,
                        "stepName", "已缓存",
                        "profile", cached
                ));
            }

            // 检查是否已在处理中
            if (cacheService.isTaskProcessing(jobName)) {
                return CommonResult.success(Map.of(
                        "jobName", jobName,
                        "status", "PROCESSING",
                        "progress", 50,
                        "stepName", "正在生成画像..."
                ));
            }

            // 标记任务开始（先清理过期的任务状态）
            cacheService.evictStaleTaskStatus();
            cacheService.startTask(jobName);
            cacheService.updateTaskProgress(jobName, 10, "正在获取岗位信息...");

            // 异步执行（使用Spring TaskExecutor确保在容器上下文中运行）
            final String jobNameFinal = jobName.trim();
            taskExecutor.execute(() -> executeAggregatedProfileGeneration(jobNameFinal));

            return CommonResult.success(Map.of(
                    "jobName", jobNameFinal,
                    "status", "PROCESSING",
                    "progress", 10,
                    "stepName", "正在初始化..."
            ));
        } catch (Exception e) {
            log.error("创建岗位画像任务失败", e);
            return CommonResult.failed("任务创建失败: " + e.getMessage());
        }
    }

    /**
     * 异步执行聚合画像生成
     */
    private void executeAggregatedProfileGeneration(String jobName) {
        try {
            cacheService.updateTaskProgress(jobName, 20, "正在从数据库加载岗位信息...");

            JobBasicInfo job = jobBasicInfoService.getOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<JobBasicInfo>()
                            .eq("job_name", jobName)
                            .last("LIMIT 1"));

            cacheService.updateTaskProgress(jobName, 40, "正在调用AI生成画像...");

            String profile;
            if (job != null) {
                profile = aiService.generateJobProfile(job);
            } else {
                profile = aiService.generateJobProfile(null);
            }

            cacheService.updateTaskProgress(jobName, 80, "正在缓存结果...");

            // 保存到缓存
            cacheService.completeTask(jobName, profile);

            log.info("岗位聚合画像生成完成: jobName={}", jobName);

        } catch (Exception e) {
            log.error("岗位聚合画像生成异常: jobName={}, error={}", jobName, e.getMessage(), e);
            cacheService.failTask(jobName, e.getMessage());
        }
    }

    /**
     * 轮询聚合画像生成状态
     */
    @GetMapping("/analysis/profile/status")
    public CommonResult<Map<String, Object>> getAggregatedProfileStatus(@RequestParam String jobName) {
        if (jobName == null || jobName.trim().isEmpty()) {
            return CommonResult.failed("岗位名称不能为空");
        }
        try {
            // 检查缓存
            String cached = cacheService.get(jobName);
            if (cached != null) {
                return CommonResult.success(Map.of(
                        "jobName", jobName,
                        "status", "COMPLETED",
                        "progress", 100,
                        "stepName", "画像已就绪",
                        "profile", cached
                ));
            }

            // 检查任务状态
            JobProfileCacheService.AsyncTaskStatus taskStatus = cacheService.getTaskStatus(jobName);
            if (taskStatus != null) {
                if ("COMPLETED".equals(taskStatus.status)) {
                    return CommonResult.success(Map.of(
                            "jobName", jobName,
                            "status", "COMPLETED",
                            "progress", 100,
                            "stepName", "画像生成完成"
                    ));
                } else if ("FAILED".equals(taskStatus.status)) {
                    return CommonResult.success(Map.of(
                            "jobName", jobName,
                            "status", "FAILED",
                            "progress", 0,
                            "stepName", "生成失败",
                            "errorMessage", taskStatus.errorMessage != null ? taskStatus.errorMessage : "未知错误"
                    ));
                } else {
                    return CommonResult.success(Map.of(
                            "jobName", jobName,
                            "status", "PROCESSING",
                            "progress", taskStatus.progress,
                            "stepName", taskStatus.stepName != null ? taskStatus.stepName : "正在生成..."
                    ));
                }
            }

            // 无缓存、无任务，返回未开始状态
            return CommonResult.success(Map.of(
                    "jobName", jobName,
                    "status", "NOT_STARTED",
                    "progress", 0,
                    "stepName", "请先发起生成请求"
            ));
        } catch (Exception e) {
            log.error("获取岗位画像状态失败", e);
            return CommonResult.failed("获取状态失败: " + e.getMessage());
        }
    }

    // 生成核心岗位的聚合画像（同步方式保留兼容）
    @GetMapping("/analysis/profile")
    public CommonResult<String> getAggregatedProfile(@RequestParam String jobName) {
        try {
            return CommonResult.success(jobBasicInfoService.generateAggregatedProfile(jobName));
        } catch (Exception e) {
            return CommonResult.failed("画像生成失败: " + e.getMessage());
        }
    }

    // 清除岗位画像缓存
    @PostMapping("/analysis/cache/clear")
    public CommonResult<String> clearCache() {
        cacheService.clear();
        return CommonResult.success("缓存已清除，当前缓存数量: " + cacheService.size());
    }

    // 根据ID获取岗位信息（仅返回名称等核心字段）
    @GetMapping("/info/{id}")
    public CommonResult<JobBasicInfo> getJobById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return CommonResult.failed("岗位ID无效");
        }
        JobBasicInfo job = jobBasicInfoService.getById(id);
        if (job == null) {
            return CommonResult.failed("岗位不存在");
        }
        return CommonResult.success(job);
    }
}
