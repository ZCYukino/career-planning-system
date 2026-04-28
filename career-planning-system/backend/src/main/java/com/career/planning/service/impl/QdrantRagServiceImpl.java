package com.career.planning.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.career.planning.common.BusinessException;
import com.career.planning.service.EmbeddingService;
import com.career.planning.service.QdrantRagService;
import jakarta.annotation.PostConstruct;
import com.career.planning.util.NullSafeUtil;
import com.career.planning.util.StringUtil;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Qdrant向量数据库RAG服务实现（REST API版）
 * 使用HTTP REST API连接Qdrant，无需额外依赖
 *
 * 知识库文件说明：
 * - data/job_profiles_15.json：主知识库，包含15个核心岗位的基础信息、职业路径、技能要求、描述
 * - data/transfer_paths_15.json：补充换岗路径数据，提供岗位之间的转换路径详情
 *
 * 初始化流程：
 * 1. Spring Boot 启动时 @PostConstruct -> init() 检查 auto-init 配置
 * 2. auto-init=true 时调用 initializeCollection()
 * 3. 懒加载兜底：semanticSearch 前检查 initialized，未初始化则自动初始化
 */
@Service
public class QdrantRagServiceImpl implements QdrantRagService {

    private static final Logger logger = LoggerFactory.getLogger(QdrantRagServiceImpl.class);

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6333}")
    private int port;

    @Value("${qdrant.collection-name:career-knowledge}")
    private String collectionName;

    @Value("${qdrant.api-key:}")
    private String apiKey;

    /**
     * 是否在 Spring Boot 启动时自动初始化向量数据库
     * 默认 true（自动初始化），设为 false 可禁用，由外部手动调用 /api/rag/init
     */
    @Value("${qdrant.auto-init:true}")
    private boolean autoInit;

    /**
     * 知识库 JSON 文件路径（相对于项目根目录或 classpath）
     * 支持两种写法：
     * - 相对路径（相对于 Spring Boot 运行目录）：data/job_profiles_15.json
     * - classpath 路径：classpath:data/job_profiles_15.json
     */
    @Value("${qdrant.knowledge-base-path:data/knowledge_base_top20.json}")
    private String knowledgeBasePath;

    /**
     * 补充换岗路径 JSON 文件路径（可选项）
     * 为每个岗位补充详细的换岗路径信息
     */
    @Value("${qdrant.transfer-paths-path:data/transfer_paths_cs20.json}")
    private String transferPathsPath;

    private static final int VECTOR_DIMENSION = 2048; // embedding-3 模型维度（智谱AI embedding-3 返回 2048 维向量）

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EmbeddingService embeddingService;

    private volatile boolean initialized = false;
    private final Set<Long> importedIds = ConcurrentHashMap.newKeySet();

    /**
     * Spring Bean 初始化完成后自动调用
     * 根据 auto-init 配置决定是否立即初始化向量数据库
     */
    @PostConstruct
    public void init() {
        logger.info("========== Qdrant RAG服务初始化 ==========");
        logger.info("Qdrant配置: {}:{}/collections/{}", host, port, collectionName);
        logger.info("自动初始化: {}", autoInit);
        logger.info("知识库文件: {}", knowledgeBasePath);
        logger.info("==========================================");

        if (autoInit) {
            logger.info("auto-init=true，开始自动初始化向量数据库...");
            initializeCollection();
        } else {
            logger.info("auto-init=false，跳过自动初始化。请手动调用 POST /api/rag/init 进行初始化");
        }
    }

    @Override
    public void initializeCollection() {
        if (initialized) {
            logger.info("集合已初始化，跳过");
            return;
        }

        try {
            // 1. 检查服务是否可用
            if (!isServiceAvailable()) {
                logger.warn("Qdrant服务不可用，将在首次使用时重试");
                return;
            }

            // 2. 检查集合是否存在
            if (!checkCollectionExists()) {
                logger.info("集合不存在，创建新集合...");
                createCollection();
            } else {
                logger.info("集合已存在: {}", collectionName);
            }

            // 3. 检查集合中是否已有数据，避免重复导入
            long existingCount = getCollectionCount();
            if (existingCount > 0) {
                logger.info("集合中已有 {} 条数据，跳过导入", existingCount);
                initialized = true;
                return;
            }

            // 4. 导入初始数据
            logger.info("集合为空，开始导入数据...");
            importInitialData();

            // 仅在导入成功后才标记为已初始化
            initialized = true;
            logger.info("Qdrant RAG服务初始化完成");

        } catch (Exception e) {
            // 初始化失败时不设置 initialized=true，允许下次重试
            logger.error("Qdrant初始化失败: {}", e.getMessage());
        }
    }

    @Override
    public void clearCollection() {
        try {
            if (!isServiceAvailable()) {
                logger.warn("Qdrant服务不可用，无法清空集合");
                return;
            }

            // 删除整个集合
            String url = String.format("http://%s:%d/collections/%s", host, port, collectionName);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
            logger.info("集合已清空: {}", collectionName);

            // 重置初始化状态
            initialized = false;

        } catch (Exception e) {
            logger.error("清空集合失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isServiceAvailable() {
        // Qdrant v1.17+ 使用根路径 "/" 作为健康检查（/health 已移除）
        // 连续探测 3 次，避免容器启动初期短暂不可用
        int attempts = 3;
        for (int i = 1; i <= attempts; i++) {
            try {
                String url = String.format("http://%s:%d/", host, port);
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return true;
                }
                logger.warn("Qdrant健康检查返回非2xx状态: {} (尝试 {}/{})", response.getStatusCode(), i, attempts);
            } catch (Exception e) {
                logger.warn("Qdrant健康检查失败: {} (尝试 {}/{})", e.getMessage(), i, attempts);
                if (i < attempts) {
                    try {
                        Thread.sleep(1000L * i); // 递增等待：1s, 2s
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查集合是否存在
     */
    private boolean checkCollectionExists() {
        try {
            String url = String.format("http://%s:%d/collections/%s", host, port, collectionName);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 创建向量集合
     */
    private void createCollection() {
        try {
            String url = String.format("http://%s:%d/collections/%s", host, port, collectionName);

            JSONObject payload = new JSONObject();
            payload.put("vectors", JSONObject.of("size", VECTOR_DIMENSION, "distance", "Cosine"));

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(payload.toJSONString(), headers);

            restTemplate.put(url, entity);
            logger.info("集合创建成功: {}", collectionName);

        } catch (Exception e) {
            logger.error("创建集合失败: {}", e.getMessage());
            throw new BusinessException("创建集合失败: " + e.getMessage());
        }
    }

    /**
     * 从JSON文件导入初始数据
     * 支持两种路径读取方式：
     * 1. 文件系统路径（相对于运行目录或绝对路径）
     * 2. classpath 路径（打包后从 JAR 内读取）
     */
    private void importInitialData() {
        try {
            // 检查是否已有数据
            long existingCount = getCollectionCount();
            if (existingCount > 0) {
                logger.info("集合中已有 {} 条数据，跳过导入", existingCount);
                initialized = true;
                return;
            }

            // 构建待导入的向量点列表
            List<JSONObject> points = new ArrayList<>();

            // 1. 读取主知识库（岗位基础信息、职业路径、技能、描述）
            String mainContent = readJsonContent(knowledgeBasePath);
            if (StringUtil.isNotBlank(mainContent)) {
                JSONObject mainData = JSON.parseObject(mainContent);
                if (mainData != null && mainData.containsKey("job_profiles")) {
                    JSONArray jobProfiles = mainData.getJSONArray("job_profiles");
                    logger.info("读取主知识库: {} 个岗位", jobProfiles.size());
                    for (int i = 0; i < jobProfiles.size(); i++) {
                        JSONObject job = jobProfiles.getJSONObject(i);
                        String jobName = job.getString("job_name");
                        int rank = job.getIntValue("rank", i + 1);
                        List<JSONObject> chunks = generateChunks(job, jobName, rank);
                        points.addAll(chunks);
                    }
                }
            } else {
                logger.warn("主知识库文件读取失败或内容为空: {}", knowledgeBasePath);
            }

            // 2. 读取补充换岗路径数据（额外的转换路径详情）
            String transferContent = readJsonContent(transferPathsPath);
            if (StringUtil.isNotBlank(transferContent)) {
                JSONArray transferArray = null;
                try {
                    transferArray = JSON.parseArray(transferContent);
                } catch (Exception e) {
                    // 如果是对象格式，提取 transfer_paths 字段
                    JSONObject transferObj = JSON.parseObject(transferContent);
                    if (transferObj != null && transferObj.containsKey("transfer_paths")) {
                        transferArray = transferObj.getJSONArray("transfer_paths");
                    }
                }
                if (transferArray != null && !transferArray.isEmpty()) {
                    logger.info("读取补充换岗路径: {} 条", transferArray.size());
                    List<JSONObject> transferChunks = generateTransferPathChunks(transferArray);
                    points.addAll(transferChunks);
                }
            } else {
                logger.warn("补充换岗路径文件读取失败或内容为空: {}", transferPathsPath);
            }

            // 3. 批量插入
            if (points.isEmpty()) {
                logger.error("没有有效的向量点需要插入，请检查知识库文件是否存在且格式正确");
                return;
            }

            upsertPoints(points);
            logger.info("初始数据导入完成，共导入 {} 条向量", points.size());
            initialized = true;

        } catch (Exception e) {
            logger.error("导入初始数据失败: {}", e.getMessage());
        }
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

        // 方式2：尝试文件系统路径（绝对路径或相对路径）
        String filePath = path;
        if (path.startsWith("classpath:")) {
            filePath = path.substring(10);
        }
        try {
            Path absPath = Paths.get(filePath);
            if (Files.exists(absPath)) {
                String content = new String(Files.readAllBytes(absPath), StandardCharsets.UTF_8);
                logger.info("从文件系统读取: {}", absPath.toAbsolutePath());
                return content;
            }
        } catch (Exception e) {
            logger.warn("文件系统读取失败: {}", e.getMessage());
        }

        logger.error("无法读取文件: {}（既不存在于文件系统，也不在 classpath 中）", path);
        return "";
    }

    /**
     * 从补充换岗路径数据生成向量片段
     * 支持两种格式：
     * 1. 直接格式：[{source_job_name, target_job_name, ...}]
     * 2. 嵌套格式：[{from_job, paths: [{target, difficulty, skills, duration, advantage}, ...]}]
     */
    private List<JSONObject> generateTransferPathChunks(JSONArray transferPaths) {
        List<JSONObject> chunks = new ArrayList<>();
        long chunkId = 900000L; // 使用大数值 ID，与岗位向量区分

        for (int i = 0; i < transferPaths.size(); i++) {
            JSONObject transfer = transferPaths.getJSONObject(i);

            // 尝试两种格式
            String sourceJob = null;
            JSONArray pathsArray = null;

            // 格式1: 嵌套格式 {from_job, paths: [...]}
            if (transfer.containsKey("from_job") && transfer.containsKey("paths")) {
                sourceJob = transfer.getString("from_job");
                pathsArray = transfer.getJSONArray("paths");
            }
            // 格式2: 直接格式 {source_job_name, target_job_name, ...}
            else if (transfer.containsKey("source_job_name") && transfer.containsKey("target_job_name")) {
                // 单条直接处理
                String targetJob = transfer.getString("target_job_name");
                if (StringUtil.isNotBlank(sourceJob) && StringUtil.isNotBlank(targetJob)) {
                    int difficulty = transfer.getIntValue("difficulty_level", 3);
                    String skills = transfer.getString("required_skills");
                    String duration = transfer.getString("transfer_duration");
                    String advantage = transfer.getString("advantage_analysis");

                    String content = String.format("%s 转岗到 %s：需要掌握的技能：%s，转岗周期：%s，优势分析：%s，转岗难度：%d/5。",
                            sourceJob, targetJob,
                            StringUtil.defaultIfBlank(skills, "待定"),
                            StringUtil.defaultIfBlank(duration, "1-2年"),
                            StringUtil.defaultIfBlank(advantage, "有一定技术基础"),
                            difficulty);

                    JSONObject meta = new JSONObject();
                    meta.put("source_job", sourceJob);
                    meta.put("target_job", targetJob);
                    meta.put("difficulty", difficulty);
                    meta.put("path_type", "transfer_detail");

                    chunks.add(createChunk(chunkId++, "transfer_detail", content, sourceJob, meta));
                }
                continue;
            }

            // 处理嵌套格式
            if (sourceJob != null && pathsArray != null) {
                for (int j = 0; j < pathsArray.size(); j++) {
                    JSONObject path = pathsArray.getJSONObject(j);
                    String targetJob = path.getString("target");

                    if (StringUtil.isBlank(targetJob)) {
                        continue;
                    }

                    int difficulty = path.getIntValue("difficulty", 3);
                    String skills = path.getString("skills");
                    String duration = path.getString("duration");
                    String advantage = path.getString("advantage");

                    String content = String.format("%s 转岗到 %s：需要掌握的技能：%s，转岗周期：%s，优势分析：%s，转岗难度：%d/5。",
                            sourceJob, targetJob,
                            StringUtil.defaultIfBlank(skills, "待定"),
                            StringUtil.defaultIfBlank(duration, "1-2年"),
                            StringUtil.defaultIfBlank(advantage, "有一定技术基础"),
                            difficulty);

                    JSONObject meta = new JSONObject();
                    meta.put("source_job", sourceJob);
                    meta.put("target_job", targetJob);
                    meta.put("difficulty", difficulty);
                    meta.put("path_type", "transfer_detail");

                    chunks.add(createChunk(chunkId++, "transfer_detail", content, sourceJob, meta));
                }
            }
        }

        logger.info("从补充换岗路径生成了 {} 条向量", chunks.size());
        return chunks;
    }

    /**
     * 生成向量片段
     * 从岗位信息中提取 5 类内容分别向量化：
     * 1. job_basic：基础信息（薪资、职位数量）
     * 2. career_path：职业发展路径（晋升路线）
     * 3. job_skills：技能要求
     * 4. job_description：职位描述
     * 5. transfer_path：换岗路径建议
     */
    private List<JSONObject> generateChunks(JSONObject job, String jobName, int rank) {
        List<JSONObject> chunks = new ArrayList<>();
        int chunkIndex = 0;

        // 1. 基础信息向量
        String salaryRanges = "";
        if (job.containsKey("salary_ranges")) {
            JSONArray salaryArray = job.getJSONArray("salary_ranges");
            salaryRanges = (salaryArray != null && !salaryArray.isEmpty())
                    ? String.join("、", salaryArray.toList(String.class)) : "";
        }
        String basicContent = String.format("%s是计算机行业热门岗位，该岗位共有%d个职位。常见薪资范围：%s。",
                jobName, job.getIntValue("job_count", 0), salaryRanges);
        JSONObject basicMeta = new JSONObject();
        basicMeta.put("job_count", job.getIntValue("job_count", 0));
        basicMeta.put("industries", job.get("industries"));

        chunks.add(createChunk(rank * 100 + chunkIndex++, "job_basic", basicContent, jobName, basicMeta));

        // 2. 职业发展路径向量
        if (job.containsKey("career_path")) {
            JSONObject careerPath = job.getJSONObject("career_path");
            String pathContent = String.format("%s的职业发展路径：从%s开始，经过%s，晋升为%s，可发展为%s或进入管理路线%s。",
                    jobName,
                    getStringSafe(careerPath, "entry"),
                    getStringSafe(careerPath, "mid"),
                    getStringSafe(careerPath, "senior"),
                    getStringSafe(careerPath, "expert"),
                    getStringSafe(careerPath, "management"));

            JSONObject pathMeta = new JSONObject();
            pathMeta.put("path_type", "vertical_promotion");

            chunks.add(createChunk(rank * 100 + chunkIndex++, "career_path", pathContent, jobName, pathMeta));
        }

            // 3. 技能要求向量
        if (job.containsKey("related_skills")) {
            JSONArray skills = job.getJSONArray("related_skills");
            if (skills != null && !skills.isEmpty()) {
                List<String> skillList = NullSafeUtil.getOrEmptyList(skills).stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                String skillsContent = jobName + "需要掌握的核心技能包括：" +
                        String.join("、", skillList) + "。";

                JSONObject skillsMeta = new JSONObject();
                skillsMeta.put("skills_count", skills.size());
                skillsMeta.put("skills", skills);

                chunks.add(createChunk(rank * 100 + chunkIndex++, "job_skills", skillsContent, jobName, skillsMeta));
            }
        }

        // 4. 职位描述向量
        if (job.containsKey("job_description")) {
            String description = job.getString("job_description");
            if (description != null && description.length() > 50) {
                if (description.length() > 1000) {
                    description = description.substring(0, 1000) + "...";
                }

                JSONObject descMeta = new JSONObject();
                descMeta.put("desc_length", description.length());

                chunks.add(createChunk(rank * 100 + chunkIndex++, "job_description", description, jobName, descMeta));
            }
        }

        // 5. 换岗路径向量（优先使用内嵌的 transfer_paths，其次使用通用描述）
        if (job.containsKey("transfer_paths")) {
            JSONArray transferPaths = job.getJSONArray("transfer_paths");
            if (transferPaths != null && !transferPaths.isEmpty()) {
                for (int t = 0; t < Math.min(transferPaths.size(), 3); t++) {
                    JSONObject tp = transferPaths.getJSONObject(t);
                    String target = tp.getString("target");
                    String skills = tp.getString("skills");
                    String duration = tp.getString("duration");
                    String advantage = tp.getString("advantage");

                    String content = String.format("%s可以转换到%s：需要掌握的技能：%s，转岗周期：%s，优势：%s。",
                            jobName, StringUtil.defaultIfBlank(target, "相关岗位"),
                            StringUtil.defaultIfBlank(skills, "待定"),
                            StringUtil.defaultIfBlank(duration, "1-2年"),
                            StringUtil.defaultIfBlank(advantage, "有一定基础"));

                    JSONObject meta = new JSONObject();
                    meta.put("target_job", target);
                    meta.put("path_type", "horizontal_transfer");

                    chunks.add(createChunk(rank * 100 + chunkIndex++, "transfer_path", content, jobName, meta));
                }
            }
        } else {
            // 没有内嵌换岗路径，使用通用描述
            String transferContent = jobName + "可以转换到其他相关岗位，如全栈工程师、技术经理、产品经理等岗位，转换周期通常为1-3年。";
            JSONObject transferMeta = new JSONObject();
            transferMeta.put("path_type", "horizontal_transfer");

            chunks.add(createChunk(rank * 100 + chunkIndex++, "transfer_path", transferContent, jobName, transferMeta));
        }

        return chunks;
    }

    private String getStringSafe(JSONObject obj, String key) {
        return obj.containsKey(key) ? obj.getString(key) : "";
    }

    private JSONObject createChunk(long id, String type, String content, String jobName, JSONObject metadata) {
        JSONObject chunk = new JSONObject();
        chunk.put("id", id);
        chunk.put("vector_type", type);
        chunk.put("content", content);
        chunk.put("metadata", metadata.toJSONString());
        chunk.put("job_name", jobName != null ? jobName : "");
        return chunk;
    }

    /**
     * 批量插入向量点
     * 过滤掉空内容和零向量点（空文本产生的零向量会严重降低检索质量）
     * 分批插入避免请求体过大（每批3条，向量数据量大）
     *
     * Qdrant v1.17 批量 upsert 格式（PUT 方法）：
     * PUT /collections/{name}/points
     * Body: {"points": [{"id": 1, "vector": [...], "payload": {...}}, ...]}
     * 每个 point 必须包含顶层 id, vector, payload
     */
    private void upsertPoints(List<JSONObject> points) {
        List<JSONObject> validPoints = new ArrayList<>();
        int skippedEmpty = 0;
        int skippedZero = 0;

        for (JSONObject point : points) {
            String content = point.getString("content");
            if (StringUtil.isBlank(content)) {
                skippedEmpty++;
                continue;
            }

            float[] vector = embeddingService.embed(content);
            if (isZeroVector(vector)) {
                skippedZero++;
                continue;
            }

            // 转换 float[] 为 List<Float>
            List<Float> vectorList = new ArrayList<>();
            for (float v : vector) {
                vectorList.add(v);
            }

            // 构建 Qdrant v1.17 PUT 格式：每个 point 包含顶层 id, vector, payload
            JSONObject qdrantPoint = new JSONObject();
            qdrantPoint.put("id", point.getLong("id")); // 顶层 id（必须）
            qdrantPoint.put("vector", vectorList);      // 顶层 vector（必须）
            qdrantPoint.put("payload", buildPayload(point, content)); // 顶层 payload（不含id）
            validPoints.add(qdrantPoint);
        }

        if (validPoints.isEmpty()) {
            logger.error("没有有效的向量点需要插入（跳过 {} 个空内容，{} 个零向量）", skippedEmpty, skippedZero);
            return;
        }

        // 分批插入，每批3条
        int batchSize = 3;
        int totalInserted = 0;
        for (int i = 0; i < validPoints.size(); i += batchSize) {
            int end = Math.min(i + batchSize, validPoints.size());
            List<JSONObject> batch = validPoints.subList(i, end);
            try {
                insertBatch(batch);
                totalInserted += batch.size();
                logger.info("已插入第 {}/{} 批 ({} 条)", (i / batchSize) + 1,
                        (validPoints.size() + batchSize - 1) / batchSize, batch.size());
            } catch (Exception e) {
                logger.error("批量插入失败 (批次 {}/{}): {}", (i / batchSize) + 1,
                        (validPoints.size() + batchSize - 1) / batchSize, e.getMessage());
            }
        }

        logger.info("批量插入完成: {} 条向量（跳过 {} 个空内容，{} 个零向量）",
                totalInserted, skippedEmpty, skippedZero);
    }

    /**
     * 构建 Qdrant point 的 payload（不含 id）
     */
    private JSONObject buildPayload(JSONObject point, String content) {
        JSONObject payload = new JSONObject();
        payload.put("vector_type", point.getString("vector_type"));
        payload.put("content", content);
        payload.put("job_name", point.getString("job_name"));
        payload.put("metadata", point.getString("metadata"));
        return payload;
    }

    /**
     * 插入单批向量点到 Qdrant
     * Qdrant v1.17 PUT 格式: {"points": [{"id": ..., "vector": [...], "payload": {...}}, ...]}
     * ⚠️ 关键：必须添加 wait=true 参数，确保数据同步持久化后再返回
     *        不加 wait=true 时，Qdrant 返回 acknowledged 但数据可能未真正写入
     */
    private void insertBatch(List<JSONObject> batch) {
        String url = String.format("http://%s:%d/collections/%s/points?wait=true", host, port, collectionName);

        // 构建 Qdrant v1.17 PUT 格式的请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("points", batch); // 每个 point 含顶层 id, vector, payload

        HttpHeaders headers = createHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

        // ⚠️ Qdrant v1.17 批量 upsert 必须用 PUT
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

        // 检查 HTTP 状态码
        if (!response.getStatusCode().is2xxSuccessful()) {
            String errorBody = response.getBody();
            throw new BusinessException("Qdrant 批量插入失败: HTTP " + response.getStatusCode().value()
                    + " - " + errorBody);
        }

        // 检查响应体中的 status 字段（wait=true 时即使 HTTP 200 也可能有业务错误）
        String responseBody = response.getBody();
        if (responseBody != null && responseBody.contains("\"error\"")) {
            JSONObject respJson = JSON.parseObject(responseBody);
            String errorMsg = respJson.getJSONObject("status") != null
                    ? respJson.getJSONObject("status").getString("error")
                    : responseBody;
            throw new BusinessException("Qdrant 批量插入失败: " + errorMsg);
        }
    }

    /**
     * 判断向量是否为零向量
     */
    private boolean isZeroVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return true;
        }
        for (float v : vector) {
            if (v != 0.0f) {
                return false;
            }
        }
        return true;
    }

    /**
     * 语义搜索
     * ⚠️ 懒加载兜底：如果尚未初始化，自动尝试初始化
     */
    @Override
    public List<SearchResult> semanticSearch(String query, int topK) {
        // 懒加载兜底：未初始化时自动尝试初始化
        if (!initialized) {
            logger.warn("检测到向量数据库尚未初始化，自动进行懒加载初始化...");
            synchronized (this) {
                if (!initialized) {
                    initializeCollection();
                }
            }
        }

        try {
            // 1. 参数校验
            if (StringUtil.isBlank(query)) {
                throw new IllegalArgumentException("搜索查询不能为空");
            }
            if (topK <= 0) {
                topK = 5; // 默认值
                logger.warn("topK 参数不合法，使用默认值 5");
            }

            // 2. 将查询文本转换为向量
            float[] queryVector = embeddingService.embed(query);
            if (isZeroVector(queryVector)) {
                throw new BusinessException("文本向量化服务异常，无法执行搜索");
            }

            // 手动转换 float[] 为 List<Float>
            List<Float> vectorList = new ArrayList<>();
            for (float v : queryVector) {
                vectorList.add(v);
            }

            // 3. 执行向量搜索
            String url = String.format("http://%s:%d/collections/%s/points/search", host, port, collectionName);

            JSONObject requestBody = new JSONObject();
            requestBody.put("vector", vectorList);
            requestBody.put("limit", topK);
            requestBody.put("with_payload", true);

            HttpHeaders headers = createHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // 4. 检查响应状态
            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Qdrant 搜索请求失败: HTTP {}", response.getStatusCode().value());
                throw new BusinessException("Qdrant 搜索请求失败");
            }

            // 5. 检查响应体
            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                logger.warn("Qdrant 搜索返回空响应");
                return new ArrayList<>();
            }

            // 6. 解析响应
            // Qdrant 响应格式: {"result": [...], "status": "ok", "time": ...}
            // 不是纯数组，需要先解析为对象再取 result 字段
            JSONObject responseJson = JSON.parseObject(responseBody);
            if (responseJson == null || !responseJson.containsKey("result")) {
                logger.warn("Qdrant 搜索响应格式错误或无 result 字段: {}", responseBody.substring(0, Math.min(200, responseBody.length())));
                return new ArrayList<>();
            }

            JSONArray results = responseJson.getJSONArray("result");

            List<SearchResult> searchResults = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                JSONObject result = results.getJSONObject(i);
                SearchResult sr = new SearchResult();

                sr.setId(String.valueOf(result.getLong("id")));
                sr.setScore(result.getDouble("score"));

                JSONObject payload = result.getJSONObject("payload");
                if (payload != null) {
                    sr.setJobName(payload.getString("job_name"));
                    sr.setVectorType(payload.getString("vector_type"));
                    sr.setContent(payload.getString("content"));
                    sr.setMetadata(payload.getString("metadata"));
                }

                searchResults.add(sr);
            }

            return searchResults;

        } catch (BusinessException | IllegalArgumentException e) {
            // 业务异常和参数异常：向上传播
            throw e;
        } catch (Exception e) {
            logger.error("语义搜索失败: query={}, error={}", query, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public String getJobContext(String jobName) {
        if (!initialized) {
            initializeCollection();
        }
        try {
            List<SearchResult> results = semanticSearch(jobName, 10);

            if (results.isEmpty()) {
                return "";
            }

            StringBuilder context = new StringBuilder();
            context.append("## 岗位知识库上下文\n\n");

            for (SearchResult result : results) {
                context.append("### [").append(result.getVectorType()).append("] ")
                        .append(result.getJobName()).append("\n");
                context.append(result.getContent()).append("\n\n");
            }

            return context.toString();

        } catch (Exception e) {
            logger.error("获取岗位上下文失败: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public String getCareerPathContext(String jobName) {
        if (!initialized) {
            initializeCollection();
        }
        try {
            String query = jobName + " 职业发展 晋升路径";
            List<SearchResult> results = semanticSearch(query, 5);

            StringBuilder context = new StringBuilder();
            context.append("## 职业发展路径\n\n");

            for (SearchResult result : results) {
                if ("career_path".equals(result.getVectorType())) {
                    context.append(result.getContent()).append("\n\n");
                }
            }

            return context.toString();

        } catch (Exception e) {
            logger.error("获取职业路径失败: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public String getTransferPathContext(String jobName) {
        if (!initialized) {
            initializeCollection();
        }
        try {
            String query = jobName + " 换岗 转换 其他岗位";
            List<SearchResult> results = semanticSearch(query, 5);

            StringBuilder context = new StringBuilder();
            context.append("## 换岗路径建议\n\n");

            for (SearchResult result : results) {
                String vectorType = result.getVectorType();
                if ("transfer_path".equals(vectorType) || "transfer_detail".equals(vectorType)) {
                    context.append(result.getContent()).append("\n\n");
                }
            }

            return context.toString();

        } catch (Exception e) {
            logger.error("获取换岗路径失败: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public String getStats() {
        try {
            String url = String.format("http://%s:%d/collections/%s", host, port, collectionName);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());
                if (result == null || !result.containsKey("result")) {
                    return "获取统计失败：响应格式错误";
                }
                JSONObject resultData = result.getJSONObject("result");
                if (resultData == null) {
                    return "获取统计失败：响应结构错误";
                }
                // Qdrant v1.17: points_count 是直接数字，不是嵌套对象
                long vectorsCount = resultData.containsKey("points_count")
                        ? resultData.getLong("points_count") : 0;

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Qdrant向量统计:\n"));
                sb.append(String.format("- 集合名称: %s\n", collectionName));
                sb.append(String.format("- 向量数量: %d\n", vectorsCount));
                sb.append(String.format("- 向量维度: %d\n", VECTOR_DIMENSION));
                sb.append(String.format("- 初始化状态: %s\n", initialized ? "已初始化" : "未初始化"));
                sb.append(String.format("- 知识库文件: %s\n", knowledgeBasePath));
                return sb.toString();
            }

            return "获取统计失败：HTTP状态码异常";

        } catch (Exception e) {
            return "Qdrant服务不可用: " + e.getMessage();
        }
    }

    /**
     * 获取集合中的向量数量
     */
    private long getCollectionCount() {
        try {
            String url = String.format("http://%s:%d/collections/%s", host, port, collectionName);
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());
                if (result == null || !result.containsKey("result")) {
                    return 0;
                }
                JSONObject resultData = result.getJSONObject("result");
                if (resultData == null) {
                    return 0;
                }
                // Qdrant v1.17: points_count 是直接数字，不是 {count: N} 对象
                if (resultData.containsKey("points_count")) {
                    return resultData.getLong("points_count");
                }
                return 0;
            }

            return 0;

        } catch (Exception e) {
            logger.warn("获取集合数量失败: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * 创建HTTP请求头
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (StringUtil.isNotBlank(apiKey)) {
            headers.set("api-key", apiKey);
        }
        return headers;
    }
}
