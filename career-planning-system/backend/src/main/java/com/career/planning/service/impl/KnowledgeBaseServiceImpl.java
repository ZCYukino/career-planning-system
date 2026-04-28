package com.career.planning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.career.planning.entity.JobPromotionRelation;
import com.career.planning.entity.JobTransferPath;
import com.career.planning.entity.KnowledgeVector;
import com.career.planning.entity.LocalKnowledgeBase;
import com.career.planning.mapper.KnowledgeBaseMapper;
import com.career.planning.service.JobPromotionRelationService;
import com.career.planning.service.JobTransferPathService;
import com.career.planning.service.KnowledgeBaseService;
import com.career.planning.service.KnowledgeVectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库服务实现
 * 支持从JSON向量数据库检索和全文本检索
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, LocalKnowledgeBase>
        implements KnowledgeBaseService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeBaseServiceImpl.class);

    @Autowired
    private JobPromotionRelationService jobPromotionRelationService;

    @Autowired
    private JobTransferPathService jobTransferPathService;

    @Autowired
    private KnowledgeVectorService knowledgeVectorService;

    @PostConstruct
    public void init() {
        logger.info("========== 知识库服务初始化 ==========");
        // 初始化职业路径数据
        syncCareerPathsToDb();
        syncTransferPathsToDb();
        logger.info("==========================================");
    }

    @Override
    public void syncFromJson() {
        // 新架构使用KnowledgeVectorService导入，不再从MD文件同步
        logger.info("知识库已切换为JSON向量数据库模式，请使用 /api/knowledge/import 接口导入数据");
    }

    @Override
    public List<LocalKnowledgeBase> getByCategory(String category) {
        // 兼容旧接口，查询知识库
        return list(new QueryWrapper<LocalKnowledgeBase>()
                .eq("category", category)
                .orderByDesc("created_at"));
    }

    @Override
    public List<LocalKnowledgeBase> search(String keyword) {
        // 兼容旧接口，使用向量服务搜索
        return list(new QueryWrapper<LocalKnowledgeBase>()
                .like("title", keyword)
                .or()
                .like("content", keyword)
                .or()
                .like("keywords", keyword)
                .orderByDesc("created_at"));
    }

    @Override
    public String getContextForReport(String jobName) {
        // 使用新的向量服务获取上下文
        return knowledgeVectorService.getJobContext(jobName);
    }

    @Override
    public List<KnowledgeVector> getCareerPaths(String jobName) {
        // 获取职业路径向量
        return knowledgeVectorService.getByJobNameAndType(jobName, "career_path");
    }

    @Override
    public List<KnowledgeVector> getTransferPaths(String jobName) {
        // 获取换岗路径向量
        return knowledgeVectorService.getByJobNameAndType(jobName, "transfer_path");
    }

    @Override
    public void syncCareerPathsToDb() {
        logger.info("开始同步职业路径数据...");

        try {
            // 从向量数据库获取所有职业路径
            List<KnowledgeVector> careerPaths = knowledgeVectorService.list(
                    new QueryWrapper<KnowledgeVector>()
                            .eq("vector_type", "career_path")
                            .orderByAsc("`rank`")
            );

            // 定义职业路径映射
            List<JobPromotionRelation> promotionRelations = new ArrayList<>();

            for (KnowledgeVector vector : careerPaths) {
                String jobName = vector.getJobName();
                if (jobName == null) continue;

                // 为每个岗位创建晋升关系
                JobPromotionRelation relation = createPromotionRelation(jobName);
                if (relation != null) {
                    promotionRelations.add(relation);
                }
            }

            // 如果向量数据为空，使用默认数据
            if (promotionRelations.isEmpty()) {
                promotionRelations = getDefaultPromotionRelations();
            }

            // 保存到数据库
            for (JobPromotionRelation relation : promotionRelations) {
                QueryWrapper<JobPromotionRelation> query = new QueryWrapper<>();
                query.eq("current_job_name", relation.getCurrentJobName())
                        .or()
                        .eq("next_job_name", relation.getNextJobName());

                // 使用 getOne(query, false) 禁用严格模式，即使有多条记录也只返回第一条
                JobPromotionRelation existing = jobPromotionRelationService.getOne(query, false);
                if (existing != null) {
                    relation.setId(existing.getId());
                    jobPromotionRelationService.updateById(relation);
                } else {
                    jobPromotionRelationService.save(relation);
                }
            }

            logger.info("职业路径同步完成，共同步 {} 条记录", promotionRelations.size());

        } catch (Exception e) {
            logger.error("同步职业路径失败", e);
        }
    }

    @Override
    public void syncTransferPathsToDb() {
        logger.info("开始同步换岗路径数据...");

        try {
            // 从向量数据库获取所有换岗路径
            List<KnowledgeVector> transferPaths = knowledgeVectorService.list(
                    new QueryWrapper<KnowledgeVector>()
                            .eq("vector_type", "transfer_path")
                            .orderByAsc("`rank`")
            );

            List<JobTransferPath> pathsToSave = new ArrayList<>();

            for (KnowledgeVector vector : transferPaths) {
                String jobName = vector.getJobName();
                if (jobName == null) continue;

                // 创建换岗关系
                JobTransferPath path = createTransferPath(jobName);
                if (path != null) {
                    pathsToSave.add(path);
                }
            }

            // 如果向量数据为空，使用默认数据
            if (pathsToSave.isEmpty()) {
                pathsToSave = getDefaultTransferPaths();
            }

            // 保存到数据库
            for (JobTransferPath path : pathsToSave) {
                QueryWrapper<JobTransferPath> query = new QueryWrapper<>();
                query.eq("source_job_name", path.getSourceJobName())
                        .eq("target_job_name", path.getTargetJobName());

                // 使用 getOne(query, false) 禁用严格模式，即使有多条记录也只返回第一条
                // 这样可以避免数据库中存在重复数据时抛出 TooManyResultsException
                JobTransferPath existing = jobTransferPathService.getOne(query, false);
                if (existing != null) {
                    path.setId(existing.getId());
                    jobTransferPathService.updateById(path);
                } else {
                    jobTransferPathService.save(path);
                }
            }

            logger.info("换岗路径同步完成，共同步 {} 条记录", pathsToSave.size());

        } catch (Exception e) {
            logger.error("同步换岗路径失败", e);
        }
    }

    /**
     * 创建晋升关系对象
     */
    private JobPromotionRelation createPromotionRelation(String jobName) {
        try {
            JobPromotionRelation relation = new JobPromotionRelation();
            relation.setCurrentJobName(jobName + "（初级）");
            relation.setNextJobName(jobName + "（高级）");
            relation.setPromotionPathDesc("通过积累项目经验和深入技术学习，提升到高级" + jobName);
            relation.setRequiredSkills("深入掌握核心技术、丰富的项目经验、良好的沟通能力");
            relation.setExperienceYears("3-5年");
            relation.setSalaryIncreaseRange("50-100%");
            relation.setCreatedAt(LocalDateTime.now());
            return relation;
        } catch (Exception e) {
            logger.warn("创建晋升关系失败: {}", jobName);
            return null;
        }
    }

    /**
     * 创建换岗路径对象
     */
    private JobTransferPath createTransferPath(String jobName) {
        try {
            JobTransferPath path = new JobTransferPath();
            path.setSourceJobId(0L);  // 默认值
            path.setSourceJobName(jobName);
            path.setTargetJobName("产品经理");
            path.setDifficultyLevel(3);
            path.setRequiredSkills("产品设计思维、需求分析能力、用户研究能力");
            path.setTransferDuration("1-2年");
            path.setAdvantageAnalysis(jobName + "的技术背景有助于理解产品实现");
            path.setCreatedAt(LocalDateTime.now());
            return path;
        } catch (Exception e) {
            logger.warn("创建换岗路径失败: {}", jobName);
            return null;
        }
    }

    /**
     * 获取默认晋升关系列表
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
            relation.setCreatedAt(LocalDateTime.now());
            relations.add(relation);
        }

        return relations;
    }

    /**
     * 获取默认换岗路径列表
     */
    private List<JobTransferPath> getDefaultTransferPaths() {
        List<JobTransferPath> paths = new ArrayList<>();

        Object[][] transferPaths = {
                {"Java开发工程师", "大数据工程师", 3, "Hadoop、Spark", "1-2年", "Java基础可复用"},
                {"Java开发工程师", "架构师", 4, "架构设计", "3-5年", "技术深度积累"},
                {"Java开发工程师", "技术经理", 4, "团队管理", "2-3年", "技术背景助力"},
                {"前端开发工程师", "全栈工程师", 3, "Node.js", "6-12月", "前端技术可复用"},
                {"前端开发工程师", "UI设计师", 3, "设计工具", "1-2年", "界面理解深入"},
                {"测试工程师", "测试开发工程师", 3, "编程能力", "6-12月", "测试经验结合开发"},
                {"测试工程师", "产品经理", 3, "产品设计", "1-2年", "质量意识强"},
                {"Python开发工程师", "数据分析师", 2, "数据分析", "3-6月", "Python可复用"},
                {"算法工程师", "AI专家", 4, "深度学习", "2-3年", "AI基础扎实"},
                {"运维工程师", "DevOps工程师", 3, "CI/CD", "6-12月", "运维基础可复用"}
        };

        for (Object[] path : transferPaths) {
            JobTransferPath transferPath = new JobTransferPath();
            transferPath.setSourceJobId(0L);  // 默认值
            transferPath.setSourceJobName((String) path[0]);
            transferPath.setTargetJobName((String) path[1]);
            transferPath.setDifficultyLevel((Integer) path[2]);
            transferPath.setRequiredSkills((String) path[3]);
            transferPath.setTransferDuration((String) path[4]);
            transferPath.setAdvantageAnalysis((String) path[5]);
            transferPath.setCreatedAt(LocalDateTime.now());
            paths.add(transferPath);
        }

        return paths;
    }
}
