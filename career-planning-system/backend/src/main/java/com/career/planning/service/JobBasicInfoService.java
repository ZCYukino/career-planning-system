package com.career.planning.service;

import com.career.planning.entity.JobBasicInfo;
import com.career.planning.entity.JobSkillTag;

import java.util.List;
import java.util.Map;

public interface JobBasicInfoService extends com.baomidou.mybatisplus.extension.service.IService<JobBasicInfo> {
    
    /**
     * 获取核心岗位列表（聚合后的Top岗位）
     * @return 岗位名称及其出现次数
     */
    List<Map<String, Object>> getTopJobs(int limit);

    /**
     * 为核心岗位生成聚合画像
     * @param jobName 核心岗位名称
     * @return 聚合后的画像信息
     */
    String generateAggregatedProfile(String jobName);
}
