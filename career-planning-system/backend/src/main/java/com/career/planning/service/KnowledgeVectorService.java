package com.career.planning.service;

import com.career.planning.entity.KnowledgeVector;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 知识库向量服务接口
 * 提供RAG检索功能
 */
public interface KnowledgeVectorService extends IService<KnowledgeVector> {

    /**
     * 从JSON文件导入知识库向量数据
     * @param jsonFilePath JSON文件路径
     * @return 导入的记录数
     */
    int importFromJson(String jsonFilePath);

    /**
     * 根据关键词检索相关知识
     * @param keyword 搜索关键词
     * @param limit 返回数量限制
     * @return 匹配的知识向量列表
     */
    List<KnowledgeVector> searchByKeyword(String keyword, int limit);

    /**
     * 获取岗位的完整上下文信息
     * @param jobName 岗位名称
     * @return 包含所有相关片段的上下文字符串
     */
    String getJobContext(String jobName);

    /**
     * 根据类型获取岗位相关向量
     * @param jobName 岗位名称
     * @param vectorType 向量类型
     * @return 知识向量列表
     */
    List<KnowledgeVector> getByJobNameAndType(String jobName, String vectorType);

    /**
     * 导出向量数据库（JSON格式）
     * @param outputPath 输出路径
     */
    void exportToJson(String outputPath);

    /**
     * 清空所有向量数据
     */
    void clearAll();

    /**
     * 获取向量统计信息
     * @return 统计信息字符串
     */
    String getStatistics();
}
