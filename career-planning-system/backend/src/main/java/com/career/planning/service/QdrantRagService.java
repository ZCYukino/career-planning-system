package com.career.planning.service;

import java.util.List;

/**
 * Qdrant向量数据库RAG服务接口
 * 提供语义检索、上下文获取等功能
 */
public interface QdrantRagService {

    /**
     * 初始化向量数据库集合
     */
    void initializeCollection();

    /**
     * 清空向量数据库集合
     */
    void clearCollection();

    /**
     * 检查Qdrant服务是否可用
     */
    boolean isServiceAvailable();

    /**
     * 语义搜索
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 搜索结果列表
     */
    List<SearchResult> semanticSearch(String query, int topK);

    /**
     * 根据岗位名称获取完整上下文
     */
    String getJobContext(String jobName);

    /**
     * 获取职业发展路径
     */
    String getCareerPathContext(String jobName);

    /**
     * 获取换岗路径
     */
    String getTransferPathContext(String jobName);

    /**
     * 获取服务统计信息
     */
    String getStats();

    /**
     * 搜索结果封装类
     */
    class SearchResult {
        private String id;
        private String content;
        private String jobName;
        private String vectorType;
        private double score;
        private String metadata;

        public SearchResult() {}

        public SearchResult(String id, String content, String jobName, String vectorType, double score, String metadata) {
            this.id = id;
            this.content = content;
            this.jobName = jobName;
            this.vectorType = vectorType;
            this.score = score;
            this.metadata = metadata;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getJobName() { return jobName; }
        public void setJobName(String jobName) { this.jobName = jobName; }
        public String getVectorType() { return vectorType; }
        public void setVectorType(String vectorType) { this.vectorType = vectorType; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getMetadata() { return metadata; }
        public void setMetadata(String metadata) { this.metadata = metadata; }

        @Override
        public String toString() {
            return String.format("SearchResult{id='%s', jobName='%s', type='%s', score=%.4f}",
                    id, jobName, vectorType, score);
        }
    }
}
