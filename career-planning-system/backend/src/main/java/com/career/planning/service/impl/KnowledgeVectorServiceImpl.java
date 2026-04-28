package com.career.planning.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.career.planning.common.BusinessException;
import com.career.planning.entity.KnowledgeVector;
import com.career.planning.mapper.KnowledgeVectorMapper;
import com.career.planning.service.KnowledgeVectorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.career.planning.util.NullSafeUtil;
import com.career.planning.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/**
 * 知识库向量服务实现
 * 支持从JSON导入、RAG检索、向量导出等功能
 */
@Service
public class KnowledgeVectorServiceImpl extends ServiceImpl<KnowledgeVectorMapper, KnowledgeVector>
        implements KnowledgeVectorService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeVectorServiceImpl.class);

    /**
     * 默认知识库JSON文件路径（使用 classpath 方式）
     */
    private static final String DEFAULT_JSON_PATH = "classpath:data/job_profiles_15.json";

    @PostConstruct
    public void init() {
        logger.info("========== 知识库向量服务初始化 ==========");
        // 只在知识库为空时从JSON导入（避免覆盖已有的完整数据）
        if (count() == 0) {
            logger.info("知识库为空，开始从JSON导入...");
            int imported = importFromJson(DEFAULT_JSON_PATH);
            logger.info("知识库导入完成，共 {} 条", imported);
        } else {
            logger.info("知识库已有 {} 条数据，跳过JSON导入", count());
        }
        logger.info("==========================================");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importFromJson(String jsonFilePath) {
        int importedCount = 0;
        try {
            String content = readJsonContent(jsonFilePath);
            if (content.isEmpty()) {
                logger.warn("知识库JSON文件读取失败: {}", jsonFilePath);
                return 0;
            }
            JSONObject jsonData = JSON.parseObject(content);

            // 清空旧数据
            clearAll();

            // 导入岗位画像
            if (jsonData.containsKey("job_profiles")) {
                JSONArray jobProfiles = jsonData.getJSONArray("job_profiles");
                for (int i = 0; i < jobProfiles.size(); i++) {
                    JSONObject job = jobProfiles.getJSONObject(i);
                    String jobName = job.getString("job_name");
                    int rank = job.getIntValue("rank", i + 1);

                    // 1. 导入基本信息向量
                    importedCount += importBasicVector(job, jobName, rank);

                    // 2. 导入职业路径向量
                    importedCount += importCareerPathVector(job, jobName, rank);

                    // 3. 导入技能要求向量
                    importedCount += importSkillsVector(job, jobName, rank);

                    // 4. 导入职位描述向量
                    importedCount += importDescriptionVector(job, jobName, rank);

                    // 5. 导入换岗路径向量
                    importedCount += importTransferPathVector(job, jobName, rank);
                }
            }

            logger.info("知识库向量导入完成，共导入 {} 条记录", importedCount);

        } catch (Exception e) {
            logger.error("导入知识库JSON失败", e);
        }
        return importedCount;
    }

    /**
     * 读取 JSON 文件内容
     * 优先尝试 classpath，失败后尝试文件系统路径
     */
    private String readJsonContent(String path) {
        // 方式1：尝试 classpath 路径（JAR 打包后可用）
        String classpath = path;
        if (classpath.startsWith("classpath:")) {
            classpath = classpath.substring(10);
        }
        try {
            ClassPathResource resource = new ClassPathResource(classpath);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    String content = StreamUtils.copyToString(is, StandardCharsets.UTF_8);
                    logger.info("从 classpath 读取: {}", classpath);
                    return content;
                }
            }
        } catch (Exception e) {
            logger.warn("classpath 读取失败: {}", e.getMessage());
        }

        // 方式2：尝试文件系统路径
        Path filePath = Paths.get(path);
        if (Files.exists(filePath)) {
            try {
                String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
                logger.info("从文件系统读取: {}", filePath.toAbsolutePath());
                return content;
            } catch (Exception e) {
                logger.warn("文件系统读取失败: {}", e.getMessage());
            }
        }

        logger.error("无法读取文件: {}（既不存在于文件系统，也不在 classpath 中）", path);
        return "";
    }

    /**
     * 导入基本信息向量
     */
    private int importBasicVector(JSONObject job, String jobName, int rank) {
        int count = 0;
        String content = String.format("%s是计算机行业热门岗位，该岗位共有%d个职位。常见薪资范围：%s。",
                jobName,
                job.getIntValue("job_count", 0),
                String.join("、", job.getJSONArray("salary_ranges").toList(String.class)));

        JSONObject metadata = new JSONObject();
        metadata.put("rank", rank);
        metadata.put("job_count", job.getIntValue("job_count", 0));
        metadata.put("industries", job.get("industries"));
        Object popScore = job.get("popularity_score");
        metadata.put("popularity_score", popScore != null ? popScore : 0);

        KnowledgeVector vector = createVector("job_basic", jobName, content, rank,
                "job_" + rank + "_basic", metadata.toJSONString());
        save(vector);
        count++;

        return count;
    }

    /**
     * 导入职业路径向量
     */
    private int importCareerPathVector(JSONObject job, String jobName, int rank) {
        int count = 0;
        JSONObject careerPath = job.getJSONObject("career_path");
        if (careerPath != null) {
            String pathText = String.format("%s的职业发展路径：从%s开始，经过%s，晋升为%s，"
                            + "可发展为%s或进入管理路线%s。",
                    jobName,
                    careerPath.getString("entry"),
                    careerPath.getString("mid"),
                    careerPath.getString("senior"),
                    careerPath.getString("expert"),
                    careerPath.getString("management"));

            JSONObject metadata = new JSONObject();
            metadata.put("rank", rank);
            metadata.put("path_type", "vertical_promotion");
            metadata.put("career_path", careerPath);

            KnowledgeVector vector = createVector("career_path", jobName, pathText, rank,
                    "job_" + rank + "_career_path", metadata.toJSONString());
            save(vector);
            count++;
        }
        return count;
    }

    /**
     * 导入技能要求向量
     */
    private int importSkillsVector(JSONObject job, String jobName, int rank) {
        int count = 0;
        JSONArray skills = job.getJSONArray("related_skills");
        if (skills != null && !skills.isEmpty()) {
            String skillsText = jobName + "需要掌握的核心技能包括：" + String.join("、", skills.toList(String.class)) + "。";

            JSONObject metadata = new JSONObject();
            metadata.put("rank", rank);
            metadata.put("skills_count", skills.size());
            metadata.put("skills", skills);

            KnowledgeVector vector = createVector("job_skills", jobName, skillsText, rank,
                    "job_" + rank + "_skills", metadata.toJSONString());
            save(vector);
            count++;
        }

        // 导入关键要求向量
        JSONObject keyRequirements = job.getJSONObject("key_requirements");
        if (keyRequirements != null) {
            StringBuilder reqText = new StringBuilder();
            reqText.append(jobName).append("的岗位要求：\n");

            JSONArray education = keyRequirements.getJSONArray("education");
            if (education != null && !education.isEmpty()) {
                reqText.append("学历要求：").append(String.join("、", education.toList(String.class))).append("\n");
            }

            JSONArray experience = keyRequirements.getJSONArray("experience");
            if (experience != null && !experience.isEmpty()) {
                reqText.append("经验要求：").append(String.join("、", experience.toList(String.class))).append("\n");
            }

            JSONArray reqSkills = keyRequirements.getJSONArray("skills");
            if (reqSkills != null && !reqSkills.isEmpty()) {
                reqText.append("技能要求：").append(String.join("、", reqSkills.toList(String.class)));
            }

            JSONObject metadata = new JSONObject();
            metadata.put("rank", rank);
            metadata.put("type", "key_requirements");

            KnowledgeVector vector = createVector("job_requirements", jobName, reqText.toString(), rank,
                    "job_" + rank + "_requirements", metadata.toJSONString());
            save(vector);
            count++;
        }

        return count;
    }

    /**
     * 导入职位描述向量
     */
    private int importDescriptionVector(JSONObject job, String jobName, int rank) {
        int count = 0;
        String description = job.getString("job_description");
        if (description != null && !description.isEmpty()) {
            // 截取前2000字符
            if (description.length() > 2000) {
                description = description.substring(0, 2000) + "...";
            }

            JSONObject metadata = new JSONObject();
            metadata.put("rank", rank);
            metadata.put("desc_length", description.length());

            KnowledgeVector vector = createVector("job_description", jobName, description, rank,
                    "job_" + rank + "_description", metadata.toJSONString());
            save(vector);
            count++;
        }
        return count;
    }

    /**
     * 导入换岗路径向量
     */
    private int importTransferPathVector(JSONObject job, String jobName, int rank) {
        int count = 0;
        // 通用换岗路径文本
        String transferText = jobName + "可以转换到其他相关岗位，如全栈工程师、技术经理、产品经理等岗位，"
                + "转换周期通常为1-3年。相关技能包括：项目管理、团队协作、业务理解等。";

        JSONObject metadata = new JSONObject();
        metadata.put("rank", rank);
        metadata.put("path_type", "horizontal_transfer");

        KnowledgeVector vector = createVector("transfer_path", jobName, transferText, rank,
                "job_" + rank + "_transfer", metadata.toJSONString());
        save(vector);
        count++;

        return count;
    }

    /**
     * 创建知识向量对象
     */
    private KnowledgeVector createVector(String type, String jobName, String content,
                                         int rank, String chunkId, String metadata) {
        KnowledgeVector vector = new KnowledgeVector();
        vector.setVectorType(type);
        vector.setJobName(jobName);
        vector.setContent(content);
        vector.setRank(rank);
        vector.setChunkId(chunkId);
        vector.setMetadata(metadata);
        vector.setCreatedAt(LocalDateTime.now());
        vector.setUpdatedAt(LocalDateTime.now());
        return vector;
    }

    @Override
    public List<KnowledgeVector> searchByKeyword(String keyword, int limit) {
        if (StringUtil.isBlank(keyword)) {
            return new ArrayList<>();
        }

        QueryWrapper<KnowledgeVector> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("job_name", keyword)
                .or()
                .like("content", keyword)
                .or()
                .like("metadata", keyword)
                .orderByDesc("`rank`")
                .last("LIMIT " + limit);

        return list(queryWrapper);
    }

    @Override
    public String getJobContext(String jobName) {
        if (StringUtil.isBlank(jobName)) {
            return "";
        }

        List<KnowledgeVector> vectors = list(new QueryWrapper<KnowledgeVector>()
                .like("job_name", jobName)
                .orderByAsc("vector_type"));

        if (NullSafeUtil.isEmpty(vectors)) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("## 岗位知识库上下文\n\n");

        // 按类型分组
        for (KnowledgeVector vector : vectors) {
            context.append("### [").append(vector.getVectorType()).append("] ")
                    .append(vector.getJobName()).append("\n");
            context.append(vector.getContent()).append("\n\n");
        }

        return context.toString();
    }

    @Override
    public List<KnowledgeVector> getByJobNameAndType(String jobName, String vectorType) {
        return list(new QueryWrapper<KnowledgeVector>()
                .eq("job_name", jobName)
                .eq("vector_type", vectorType));
    }

    @Override
    public void exportToJson(String outputPath) {
        List<KnowledgeVector> allVectors = list();

        JSONObject exportData = new JSONObject();
        exportData.put("version", "1.0");
        exportData.put("export_time", LocalDateTime.now().toString());
        exportData.put("total_vectors", allVectors.size());
        exportData.put("vectors", allVectors);

        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(JSON.toJSONString(exportData));
            logger.info("向量数据导出成功: {}", outputPath);
        } catch (IOException e) {
            logger.error("导出向量数据失败", e);
            throw new BusinessException("导出向量数据失败: " + e.getMessage());
        }
    }

    @Override
    public void clearAll() {
        remove(null);
        logger.info("已清空所有向量数据");
    }

    @Override
    public String getStatistics() {
        long totalCount = count();
        Long basicCount = baseMapper.selectCount(new QueryWrapper<KnowledgeVector>().eq("vector_type", "job_basic"));
        Long careerCount = baseMapper.selectCount(new QueryWrapper<KnowledgeVector>().eq("vector_type", "career_path"));
        Long skillsCount = baseMapper.selectCount(new QueryWrapper<KnowledgeVector>().eq("vector_type", "job_skills"));
        Long descCount = baseMapper.selectCount(new QueryWrapper<KnowledgeVector>().eq("vector_type", "job_description"));
        Long transferCount = baseMapper.selectCount(new QueryWrapper<KnowledgeVector>().eq("vector_type", "transfer_path"));

        List<Map<String, Object>> jobStats = baseMapper.selectMaps(
                new QueryWrapper<KnowledgeVector>()
                        .select("job_name", "count(*) as count")
                        .groupBy("job_name")
                        .orderByDesc("count")
                        .last("LIMIT 10"));

        StringBuilder stats = new StringBuilder();
        stats.append("========== 知识库向量统计 ==========\n");
        stats.append("总向量数: ").append(totalCount).append("\n");
        stats.append("- 基础信息向量: ").append(basicCount).append("\n");
        stats.append("- 职业路径向量: ").append(careerCount).append("\n");
        stats.append("- 技能要求向量: ").append(skillsCount).append("\n");
        stats.append("- 职位描述向量: ").append(descCount).append("\n");
        stats.append("- 换岗路径向量: ").append(transferCount).append("\n");
        stats.append("\nTop10 热门岗位:\n");

        int i = 1;
        for (Map<String, Object> stat : jobStats) {
            stats.append(i++).append(". ").append(stat.get("job_name")).append(": ").append(stat.get("count")).append("条\n");
        }

        return stats.toString();
    }
}
