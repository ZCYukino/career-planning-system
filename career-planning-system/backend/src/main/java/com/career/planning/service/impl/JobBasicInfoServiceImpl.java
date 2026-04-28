package com.career.planning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.career.planning.entity.JobBasicInfo;
import com.career.planning.mapper.JobBasicInfoMapper;
import com.career.planning.service.AIService;
import com.career.planning.service.JobBasicInfoService;
import com.career.planning.service.JobProfileCacheService;
import com.career.planning.service.KnowledgeVectorService;
import com.career.planning.service.QdrantRagService;
import com.career.planning.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class JobBasicInfoServiceImpl extends ServiceImpl<JobBasicInfoMapper, JobBasicInfo> implements JobBasicInfoService {

    private static final Logger log = LoggerFactory.getLogger(JobBasicInfoServiceImpl.class);

    @Autowired
    private JobBasicInfoMapper jobBasicInfoMapper;
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private JobProfileCacheService cacheService;

    @Autowired
    private KnowledgeVectorService knowledgeVectorService;

    @Autowired(required = false)
    private QdrantRagService qdrantRagService;

    @Override
    public List<Map<String, Object>> getTopJobs(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return jobBasicInfoMapper.selectMaps(
            new QueryWrapper<JobBasicInfo>()
                .select("job_name, MAX(COALESCE(job_count, 1)) as count")
                .groupBy("job_name")
                .orderByDesc("count")
                .last("LIMIT " + safeLimit)
        );
    }

    @Override
    public String generateAggregatedProfile(String jobName) {
        String cached = cacheService.get(jobName);
        if (cached != null) {
            log.info("命中缓存: {}", jobName);
            return cached;
        }

        List<JobBasicInfo> samples = list(new QueryWrapper<JobBasicInfo>()
                .eq("job_name", jobName)
                .last("LIMIT 20"));

        if (samples.isEmpty()) {
            return generateDefaultProfile(jobName);
        }

        StringBuilder combinedDesc = new StringBuilder();
        combinedDesc.append("基于").append(samples.size()).append("份企业招聘需求汇总：\n");

        for (int i = 0; i < samples.size(); i++) {
            JobBasicInfo job = samples.get(i);
            if (job.getJobDescription() != null && !job.getJobDescription().isEmpty()) {
                combinedDesc.append("【招聘").append(i + 1).append("】")
                    .append(job.getCompanyName() != null ? job.getCompanyName() : "某公司")
                    .append(" - ")
                    .append(job.getJobDescription())
                    .append("\n");
            }
        }

        if (combinedDesc.length() > 8000) {
            combinedDesc = new StringBuilder(combinedDesc.substring(0, 8000) + "...(内容已截断)");
        }

        // 从知识库检索该岗位的上下文（职业路径、换岗路径、技能要求等）
        // 优先使用Qdrant RAG语义搜索，fallback到KnowledgeVectorService
        String knowledgeContext = getKnowledgeContextWithFallback(jobName);

        JobBasicInfo virtualJob = new JobBasicInfo();
        virtualJob.setJobName(jobName);
        virtualJob.setJobDescription(combinedDesc.toString());

        try {
            // 使用知识库检索增强的岗位画像生成
            String profile = aiService.generateJobProfileWithKnowledge(virtualJob, knowledgeContext);
            cacheService.put(jobName, profile);
            log.info("已缓存岗位画像: {}", jobName);
            return profile;
        } catch (Exception e) {
            log.warn("岗位画像AI生成失败，使用默认画像: job={}, reason={}", jobName, e.getMessage());
            return generateDefaultProfile(jobName);
        }
    }

    /**
     * 获取知识库上下文（双重兜底：Qdrant语义搜索 → KnowledgeVectorService）
     * 优先使用Qdrant的语义搜索能力，Qdrant不可用时fallback到KnowledgeVectorService
     */
    private String getKnowledgeContextWithFallback(String jobName) {
        // 1. 优先使用 QdrantRagService 的语义搜索（更智能的RAG能力）
        if (qdrantRagService != null) {
            try {
                if (qdrantRagService.isServiceAvailable()) {
                    String qdrantContext = qdrantRagService.getJobContext(jobName);
                    if (StringUtil.isNotBlank(qdrantContext)) {
                        log.info("JobBasicInfo: 使用Qdrant RAG获取岗位上下文: {}", jobName);
                        return qdrantContext;
                    }
                }
            } catch (Exception e) {
                log.warn("JobBasicInfo: Qdrant RAG获取上下文失败，将使用备用方案: {}", e.getMessage());
            }
        }

        // 2. Fallback到 KnowledgeVectorService（基于数据库的向量检索）
        try {
            String vectorContext = knowledgeVectorService.getJobContext(jobName);
            if (StringUtil.isNotBlank(vectorContext)) {
                log.info("JobBasicInfo: Qdrant不可用，使用KnowledgeVectorService获取上下文: {}", jobName);
                return vectorContext;
            }
        } catch (Exception e) {
            log.warn("JobBasicInfo: KnowledgeVectorService获取上下文失败: {}", e.getMessage());
        }

        log.warn("JobBasicInfo: 所有知识库服务均不可用或无数据: {}", jobName);
        return "";
    }
    
    private String generateDefaultProfile(String jobName) {
        return String.format("""
            {
                "job_title": "%s",
                "professional_skills": {
                    "required": ["相关专业技能"],
                    "preferred": [],
                    "level": "中级"
                },
                "certificates": {"required": [], "preferred": []},
                "education": {"minimum": "本科", "preferred": "本科及以上", "major": ["相关专业"]},
                "experience": {"years": "不限", "type": "相关经验优先"},
                "soft_skills": {
                    "innovation_ability": {"level": 3, "description": "具备基本创新能力"},
                    "learning_ability": {"level": 4, "description": "良好的学习能力"},
                    "pressure_resistance": {"level": 3, "description": "能承受一定工作压力"},
                    "communication_ability": {"level": 4, "description": "良好的沟通能力"}
                },
                "career_path": {
                    "entry_level": "%s助理",
                    "mid_level": "高级%s",
                    "senior_level": "%s专家/经理"
                },
                "salary_reference": "面议",
                "industry_outlook": "行业发展稳定"
            }
            """, jobName, jobName, jobName, jobName);
    }
}
