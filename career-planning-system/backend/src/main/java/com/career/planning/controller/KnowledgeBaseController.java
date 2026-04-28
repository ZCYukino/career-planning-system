package com.career.planning.controller;

import com.career.planning.common.CommonResult;
import com.career.planning.entity.KnowledgeVector;
import com.career.planning.entity.LocalKnowledgeBase;
import com.career.planning.service.KnowledgeBaseService;
import com.career.planning.service.KnowledgeVectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private KnowledgeVectorService knowledgeVectorService;

    /**
     * 获取所有知识库条目
     */
    @GetMapping("/list")
    public CommonResult<List<LocalKnowledgeBase>> getAll() {
        return CommonResult.success(knowledgeBaseService.list());
    }

    /**
     * 按分类获取知识库
     */
    @GetMapping("/category/{category}")
    public CommonResult<List<LocalKnowledgeBase>> getByCategory(@PathVariable String category) {
        if (!StringUtils.hasText(category)) {
            return CommonResult.failed("分类名称不能为空");
        }
        return CommonResult.success(knowledgeBaseService.getByCategory(category.trim()));
    }

    /**
     * 搜索知识库
     */
    @GetMapping("/search")
    public CommonResult<List<LocalKnowledgeBase>> search(@RequestParam String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return CommonResult.failed("搜索关键词不能为空");
        }
        return CommonResult.success(knowledgeBaseService.search(keyword.trim()));
    }

    /**
     * 获取知识库详情
     */
    @GetMapping("/detail/{id}")
    public CommonResult<LocalKnowledgeBase> getById(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return CommonResult.failed("ID无效");
        }
        LocalKnowledgeBase kb = knowledgeBaseService.getById(id);
        if (kb != null) {
            return CommonResult.success(kb);
        }
        return CommonResult.failed("知识不存在");
    }

    /**
     * 获取岗位上下文（用于报告生成）- 兼容旧接口
     */
    @GetMapping("/context-legacy")
    public CommonResult<String> getContextForReportLegacy(@RequestParam String jobName) {
        if (!StringUtils.hasText(jobName)) {
            return CommonResult.failed("岗位名称不能为空");
        }
        String context = knowledgeBaseService.getContextForReport(jobName.trim());
        return CommonResult.success(context);
    }

    /**
     * 获取职业晋升路径
     */
    @GetMapping("/career-paths")
    public CommonResult<List<KnowledgeVector>> getCareerPaths(@RequestParam String jobName) {
        if (!StringUtils.hasText(jobName)) {
            return CommonResult.failed("岗位名称不能为空");
        }
        return CommonResult.success(knowledgeBaseService.getCareerPaths(jobName.trim()));
    }

    /**
     * 获取换岗路径
     */
    @GetMapping("/transfer-paths")
    public CommonResult<List<KnowledgeVector>> getTransferPaths(@RequestParam String jobName) {
        if (!StringUtils.hasText(jobName)) {
            return CommonResult.failed("岗位名称不能为空");
        }
        return CommonResult.success(knowledgeBaseService.getTransferPaths(jobName.trim()));
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/stats")
    public CommonResult<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        List<LocalKnowledgeBase> all = knowledgeBaseService.list();

        Map<String, Integer> categoryCount = new HashMap<>();
        int totalWords = 0;

        for (LocalKnowledgeBase kb : all) {
            if (kb.getCategory() != null) {
                categoryCount.put(kb.getCategory(),
                        categoryCount.getOrDefault(kb.getCategory(), 0) + 1);
            }
            if (kb.getContent() != null) {
                totalWords += kb.getContent().length();
            }
        }

        stats.put("totalFiles", all.size());
        stats.put("totalWords", totalWords);
        stats.put("categoryCount", categoryCount);

        return CommonResult.success(stats);
    }

    /**
     * 同步知识库
     */
    @PostMapping("/sync")
    public CommonResult<String> sync() {
        try {
            knowledgeBaseService.syncFromJson();
            return CommonResult.success("知识库已切换为JSON向量模式");
        } catch (Exception e) {
            return CommonResult.failed("同步失败: " + e.getMessage());
        }
    }

    /**
     * 同步所有数据
     */
    @PostMapping("/sync-all")
    public CommonResult<String> syncAll() {
        try {
            knowledgeBaseService.syncFromJson();
            knowledgeBaseService.syncCareerPathsToDb();
            knowledgeBaseService.syncTransferPathsToDb();
            return CommonResult.success("知识库和职业路径同步完成");
        } catch (Exception e) {
            return CommonResult.failed("同步失败: " + e.getMessage());
        }
    }

    /**
     * 同步职业路径
     */
    @PostMapping("/sync-career-paths")
    public CommonResult<String> syncCareerPaths() {
        try {
            knowledgeBaseService.syncCareerPathsToDb();
            return CommonResult.success("垂直晋升路径同步完成");
        } catch (Exception e) {
            return CommonResult.failed("同步失败: " + e.getMessage());
        }
    }

    /**
     * 同步换岗路径
     */
    @PostMapping("/sync-transfer-paths")
    public CommonResult<String> syncTransferPaths() {
        try {
            knowledgeBaseService.syncTransferPathsToDb();
            return CommonResult.success("换岗路径同步完成");
        } catch (Exception e) {
            return CommonResult.failed("同步失败: " + e.getMessage());
        }
    }

    // ==================== 向量数据库接口 ====================

    /**
     * 重新导入知识库向量
     */
    @PostMapping("/import")
    public CommonResult<Map<String, Object>> importKnowledge(
            @RequestParam(required = false, defaultValue = "data/knowledge_base_final.json") String filePath) {
        try {
            int count = knowledgeVectorService.importFromJson(filePath);
            Map<String, Object> result = new HashMap<>();
            result.put("count", count);
            result.put("message", "导入成功，共导入 " + count + " 条向量数据");
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("导入失败: " + e.getMessage());
        }
    }

    /**
     * 导出向量数据库
     */
    @PostMapping("/export")
    public CommonResult<Map<String, Object>> exportKnowledge(
            @RequestParam(required = false, defaultValue = "data/vector_database_export.json") String outputPath) {
        try {
            knowledgeVectorService.exportToJson(outputPath);
            Map<String, Object> result = new HashMap<>();
            result.put("path", outputPath);
            result.put("message", "导出成功");
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("导出失败: " + e.getMessage());
        }
    }

    /**
     * 搜索向量知识库
     */
    @GetMapping("/vector/search")
    public CommonResult<Map<String, Object>> searchVectors(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        try {
            List<KnowledgeVector> vectors = knowledgeVectorService.searchByKeyword(keyword, limit);
            Map<String, Object> result = new HashMap<>();
            result.put("data", vectors);
            result.put("count", vectors.size());
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("搜索失败: " + e.getMessage());
        }
    }

    /**
     * 获取向量统计信息
     */
    @GetMapping("/vector/stats")
    public CommonResult<String> getVectorStats() {
        try {
            String stats = knowledgeVectorService.getStatistics();
            return CommonResult.success(stats);
        } catch (Exception e) {
            return CommonResult.failed("获取统计失败: " + e.getMessage());
        }
    }

    /**
     * 清空向量知识库
     */
    @DeleteMapping("/vector/clear")
    public CommonResult<String> clearVectors() {
        try {
            knowledgeVectorService.clearAll();
            return CommonResult.success("向量知识库已清空");
        } catch (Exception e) {
            return CommonResult.failed("清空失败: " + e.getMessage());
        }
    }
}
