package com.career.planning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.career.planning.entity.JobPromotionRelation;

import java.util.List;
import java.util.Map;

public interface JobPromotionRelationService extends IService<JobPromotionRelation> {

    List<Map<String, Object>> getPromotionPathByJobName(String jobName);

    List<Map<String, Object>> getAllPromotionPaths();

    String getPromotionPathContext(String jobName);
}
