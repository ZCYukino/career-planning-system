package com.career.planning.service;

import com.career.planning.entity.KnowledgeVector;
import com.career.planning.entity.LocalKnowledgeBase;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 知识库服务接口
 * 提供基于JSON向量数据库的知识检索功能
 */
public interface KnowledgeBaseService extends IService<LocalKnowledgeBase> {

    /**
     * 同步知识库（从JSON向量数据库）
     * 新架构已切换为JSON向量数据库，此方法保留兼容
     */
    void syncFromJson();

    /**
     * 获取分类下的知识库
     */
    List<LocalKnowledgeBase> getByCategory(String category);

    /**
     * 搜索知识库
     */
    List<LocalKnowledgeBase> search(String keyword);

    /**
     * 获取岗位上下文（用于报告生成）
     * @param jobName 岗位名称
     * @return 上下文字符串
     */
    String getContextForReport(String jobName);

    /**
     * 获取职业晋升路径
     * @param jobName 岗位名称
     * @return 职业路径向量列表
     */
    List<KnowledgeVector> getCareerPaths(String jobName);

    /**
     * 获取换岗路径
     * @param jobName 岗位名称
     * @return 换岗路径向量列表
     */
    List<KnowledgeVector> getTransferPaths(String jobName);

    /**
     * 同步职业路径到数据库
     */
    void syncCareerPathsToDb();

    /**
     * 同步换岗路径到数据库
     */
    void syncTransferPathsToDb();
}
