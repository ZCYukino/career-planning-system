package com.career.planning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.career.planning.entity.LocalKnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<LocalKnowledgeBase> {

    int deleteByCategory(@Param("category") String category);

    int deleteByFilePath(@Param("filePath") String filePath);
}
