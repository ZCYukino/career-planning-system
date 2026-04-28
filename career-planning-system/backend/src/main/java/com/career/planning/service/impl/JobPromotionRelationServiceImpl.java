package com.career.planning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.career.planning.entity.JobPromotionRelation;
import com.career.planning.mapper.JobPromotionRelationMapper;
import com.career.planning.service.JobPromotionRelationService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 岗位晋升关系服务实现
 */
@Service
public class JobPromotionRelationServiceImpl extends ServiceImpl<JobPromotionRelationMapper, JobPromotionRelation>
        implements JobPromotionRelationService {

    private static final Logger log = LoggerFactory.getLogger(JobPromotionRelationServiceImpl.class);

    @Override
    public List<Map<String, Object>> getPromotionPathByJobName(String jobName) {
        return baseMapper.selectByJobNameWithJoin(jobName);
    }

    @Override
    public List<Map<String, Object>> getAllPromotionPaths() {
        return baseMapper.selectAllWithJobName();
    }

    /**
     * Spring启动时自动初始化SQL兜底数据
     * 当job_promotion_relation表为空时，自动填充默认晋升路径数据
     */
    @PostConstruct
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultData() {
        if (count() > 0) {
            log.info("job_promotion_relation表已有 {} 条数据，跳过初始化", count());
            return;
        }

        log.info("job_promotion_relation表为空，开始初始化默认晋升路径数据...");

        List<JobPromotionRelation> defaultRelations = getDefaultPromotionRelations();

        for (JobPromotionRelation relation : defaultRelations) {
            try {
                relation.setCreatedAt(LocalDateTime.now());
                save(relation);
            } catch (Exception e) {
                log.warn("插入晋升关系失败（可能已存在）: {}", relation.getCurrentJobName());
            }
        }

        log.info("job_promotion_relation表初始化完成，共插入 {} 条记录", defaultRelations.size());
    }

    /**
     * 获取默认晋升关系列表（硬编码兜底）
     */
    private List<JobPromotionRelation> getDefaultPromotionRelations() {
        List<JobPromotionRelation> relations = new ArrayList<>();

        String[][] promotionPaths = {
                {"Java开发工程师", "Java架构师", "深入架构设计", "系统架构、微服务", "5年", "100%"},
                {"Java开发工程师", "技术经理", "技术管理转型", "团队管理、项目管理", "3年", "50%"},
                {"前端开发工程师", "前端架构师", "前端技术深度", "前端架构、工程化", "5年", "80%"},
                {"Python开发工程师", "数据科学家", "数据建模能力", "机器学习、深度学习", "3年", "100%"},
                {"测试工程师", "测试架构师", "测试技术深度", "自动化测试、测试框架", "5年", "80%"},
                {"运维工程师", "DevOps工程师", "运维自动化", "CI/CD、容器化", "2年", "60%"},
                {"算法工程师", "AI专家", "算法研究深度", "前沿算法、技术创新", "5年", "100%"},
                {"产品经理", "产品总监", "产品战略规划", "产品规划、商业分析", "5年", "100%"},
                {"UI设计师", "设计总监", "设计管理能力", "设计管理、品牌设计", "5年", "80%"}
        };

        for (String[] path : promotionPaths) {
            JobPromotionRelation relation = new JobPromotionRelation();
            relation.setCurrentJobName(path[0]);
            relation.setNextJobName(path[1]);
            relation.setPromotionPathDesc(path[2]);
            relation.setRequiredSkills(path[3]);
            relation.setExperienceYears(path[4]);
            relation.setSalaryIncreaseRange(path[5]);
            relations.add(relation);
        }

        return relations;
    }

    @Override
    public String getPromotionPathContext(String jobName) {
        List<Map<String, Object>> paths = getPromotionPathByJobName(jobName);

        if (paths == null || paths.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("## 岗位垂直晋升路径\n\n");

        for (Map<String, Object> path : paths) {
            // 优先使用名称字段，如果没有则使用ID关联查询的占位符
            String currentJob = path.get("current_job_name") != null ?
                    path.get("current_job_name").toString() : "岗位" + path.get("current_job_id");
            String nextJob = path.get("next_job_name") != null ?
                    path.get("next_job_name").toString() : "岗位" + path.get("next_job_id");

            context.append("### ").append(currentJob)
                    .append(" → ").append(nextJob).append("\n");

            if (path.get("promotion_path_desc") != null) {
                context.append("**路径描述**：").append(path.get("promotion_path_desc")).append("\n");
            }
            if (path.get("required_skills") != null) {
                context.append("**所需技能**：").append(path.get("required_skills")).append("\n");
            }
            if (path.get("experience_years") != null) {
                context.append("**经验要求**：").append(path.get("experience_years")).append("\n");
            }
            if (path.get("salary_increase_range") != null) {
                context.append("**薪资涨幅**：").append(path.get("salary_increase_range")).append("\n");
            }
            context.append("\n");
        }

        return context.toString();
    }
}
