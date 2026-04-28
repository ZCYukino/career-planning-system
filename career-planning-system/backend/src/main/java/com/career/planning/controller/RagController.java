package com.career.planning.controller;

import com.career.planning.common.CommonResult;
import com.career.planning.service.QdrantRagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG向量检索控制器
 * 提供语义搜索、上下文获取等功能
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    @Autowired
    private QdrantRagService qdrantRagService;

    /**
     * 健康检查 - 检查Qdrant服务状态
     */
    @GetMapping("/health")
    public CommonResult<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        boolean available = qdrantRagService.isServiceAvailable();
        result.put("qdrant_available", available);

        if (available) {
            result.put("status", "healthy");
            result.put("message", "Qdrant服务运行正常");
        } else {
            result.put("status", "unhealthy");
            result.put("message", "Qdrant服务不可用，请检查是否启动");
        }

        return CommonResult.success(result);
    }

    /**
     * 初始化向量数据库
     */
    @PostMapping("/init")
    public CommonResult<String> init() {
        try {
            qdrantRagService.initializeCollection();
            return CommonResult.success("向量数据库初始化完成");
        } catch (Exception e) {
            return CommonResult.failed("初始化失败: " + e.getMessage());
        }
    }

    /**
     * 语义搜索
     * 使用自然语言查询，返回最相关的知识片段
     */
    @GetMapping("/search")
    public CommonResult<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int topK) {

        if (query == null || query.trim().isEmpty()) {
            return CommonResult.failed("查询内容不能为空");
        }

        try {
            List<QdrantRagService.SearchResult> results = qdrantRagService.semanticSearch(query.trim(), topK);

            Map<String, Object> response = new HashMap<>();
            response.put("query", query);
            response.put("total", results.size());
            response.put("results", results);

            return CommonResult.success(response);

        } catch (Exception e) {
            return CommonResult.failed("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取岗位上下文
     * 用于职业规划报告生成
     */
    @GetMapping("/context")
    public CommonResult<Map<String, Object>> getContext(@RequestParam String jobName) {
        if (jobName == null || jobName.trim().isEmpty()) {
            return CommonResult.failed("岗位名称不能为空");
        }

        try {
            String context = qdrantRagService.getJobContext(jobName.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("job_name", jobName);
            response.put("context", context);
            response.put("has_content", !context.isEmpty());

            return CommonResult.success(response);

        } catch (Exception e) {
            return CommonResult.failed("获取上下文失败: " + e.getMessage());
        }
    }

    /**
     * 获取职业发展路径
     */
    @GetMapping("/career-path")
    public CommonResult<Map<String, Object>> getCareerPath(@RequestParam String jobName) {
        if (jobName == null || jobName.trim().isEmpty()) {
            return CommonResult.failed("岗位名称不能为空");
        }

        try {
            String context = qdrantRagService.getCareerPathContext(jobName.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("job_name", jobName);
            response.put("career_path", context);

            return CommonResult.success(response);

        } catch (Exception e) {
            return CommonResult.failed("获取职业路径失败: " + e.getMessage());
        }
    }

    /**
     * 获取换岗路径
     */
    @GetMapping("/transfer-path")
    public CommonResult<Map<String, Object>> getTransferPath(@RequestParam String jobName) {
        if (jobName == null || jobName.trim().isEmpty()) {
            return CommonResult.failed("岗位名称不能为空");
        }

        try {
            String context = qdrantRagService.getTransferPathContext(jobName.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("job_name", jobName);
            response.put("transfer_path", context);

            return CommonResult.success(response);

        } catch (Exception e) {
            return CommonResult.failed("获取换岗路径失败: " + e.getMessage());
        }
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/stats")
    public CommonResult<String> getStats() {
        try {
            String stats = qdrantRagService.getStats();
            return CommonResult.success(stats);
        } catch (Exception e) {
            return CommonResult.failed("获取统计失败: " + e.getMessage());
        }
    }

    /**
     * 完整RAG检索（返回所有上下文）
     */
    @GetMapping("/full-context")
    public CommonResult<Map<String, Object>> getFullContext(@RequestParam String jobName) {
        if (jobName == null || jobName.trim().isEmpty()) {
            return CommonResult.failed("岗位名称不能为空");
        }

        try {
            String jobContext = qdrantRagService.getJobContext(jobName.trim());
            String careerPath = qdrantRagService.getCareerPathContext(jobName.trim());
            String transferPath = qdrantRagService.getTransferPathContext(jobName.trim());

            Map<String, Object> response = new HashMap<>();
            response.put("job_name", jobName);
            response.put("job_context", jobContext);
            response.put("career_path", careerPath);
            response.put("transfer_path", transferPath);
            response.put("full_context", jobContext + "\n\n" + careerPath + "\n\n" + transferPath);

            return CommonResult.success(response);

        } catch (Exception e) {
            return CommonResult.failed("获取完整上下文失败: " + e.getMessage());
        }
    }

    /**
     * 清空向量数据库集合并重新初始化
     */
    @DeleteMapping("/collection")
    public CommonResult<String> clearAndReinitialize() {
        try {
            qdrantRagService.clearCollection();
            qdrantRagService.initializeCollection();
            return CommonResult.success("集合已清空并重新初始化");
        } catch (Exception e) {
            return CommonResult.failed("清空并重新初始化失败: " + e.getMessage());
        }
    }
}
