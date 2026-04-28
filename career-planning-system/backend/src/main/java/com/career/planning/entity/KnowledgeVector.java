package com.career.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识库向量实体
 * 用于存储岗位知识库的向量嵌入，支持RAG检索
 */
@Data
@TableName("knowledge_vector")
public class KnowledgeVector {

    /**
     * 向量ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 向量类型：job_basic/career_path/job_skills/job_description/transfer_path
     */
    private String vectorType;

    /**
     * 关联的岗位名称
     */
    private String jobName;

    /**
     * 向量内容（文本片段）
     */
    private String content;

    /**
     * 向量嵌入（JSON格式存储的向量数组）
     * 可选：用于存储实际向量，若使用外部向量数据库可省略
     */
    private String vectorEmbedding;

    /**
     * 元数据JSON（存储额外信息如rank、job_count等）
     */
    private String metadata;

    /**
     * 关联的岗位排名
     * 注意：rank是MySQL保留字，需要使用反引号
     */
    @TableField("`rank`")
    private Integer rank;

    /**
     * 片段来源标识
     */
    private String chunkId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
