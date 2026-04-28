package com.career.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("local_knowledge_base")
public class LocalKnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String category;

    private String filePath;

    private String tags;

    private String keywords;

    private String source;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
