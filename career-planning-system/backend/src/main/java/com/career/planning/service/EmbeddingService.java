package com.career.planning.service;

import java.util.List;

/**
 * 文本嵌入服务接口
 * 用于将文本转换为向量表示，支持RAG检索
 */
public interface EmbeddingService {

    /**
     * 将单个文本转换为向量
     * @param text 输入文本
     * @return 文本对应的向量（浮点数组）
     */
    float[] embed(String text);

    /**
     * 批量将文本转换为向量
     * @param texts 输入文本列表
     * @return 向量列表
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 获取向量维度
     * @return 向量维度
     */
    int getDimension();
}
