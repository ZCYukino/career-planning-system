package com.career.planning.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.career.planning.common.BusinessException;
import com.career.planning.common.ProfileConstants;
import com.career.planning.entity.CareerPlanReport;
import com.career.planning.entity.JobBasicInfo;
import com.career.planning.entity.StudentInfo;
import com.career.planning.mapper.CareerPlanReportMapper;
import com.career.planning.service.AIService;
import com.career.planning.service.CareerPlanReportService;
import com.career.planning.service.JobBasicInfoService;
import com.career.planning.service.KnowledgeBaseService;
import com.career.planning.service.KnowledgeVectorService;
import com.career.planning.service.QdrantRagService;
import com.career.planning.service.StudentAbilityProfileService;
import com.career.planning.service.StudentInfoService;
import com.career.planning.util.StringUtil;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CareerPlanReportServiceImpl extends ServiceImpl<CareerPlanReportMapper, CareerPlanReport> implements CareerPlanReportService {

    private static final Logger log = LoggerFactory.getLogger(CareerPlanReportServiceImpl.class);

    /** 报告生成步骤名称 */
    private static final String STEP_INIT = "正在初始化...";
    private static final String STEP_STUDENT_PROFILE = "正在生成学生画像...";
    private static final String STEP_JOB_PROFILE = "正在生成岗位画像...";
    private static final String STEP_MATCH_ANALYSIS = "正在分析人岗匹配...";
    private static final String STEP_KNOWLEDGE = "正在检索知识库...";
    private static final String STEP_COMPLETE = "报告生成完成";

    /** 兜底学生画像（来自 ProfileConstants，集中管理避免重复定义） */
    private static final String FALLBACK_STUDENT_PROFILE = ProfileConstants.FALLBACK_STUDENT_PROFILE;

    @Autowired
    private StudentInfoService studentInfoService;

    @Autowired
    private StudentAbilityProfileService studentAbilityProfileService;

    @Autowired
    private JobBasicInfoService jobBasicInfoService;

    @Autowired
    private AIService aiService;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private QdrantRagService qdrantRagService;

    @Autowired
    private KnowledgeVectorService knowledgeVectorService;

    @Autowired
    @Qualifier("taskExecutor")
    private TaskExecutor taskExecutor;

    // 并行执行器：用于 Qdrant 三次检索并行化，避免串行等待
    private final ExecutorService parallelExecutor = Executors.newFixedThreadPool(3);

    /**
     * Spring Bean 销毁时关闭线程池，防止内存泄漏
     */
    @PreDestroy
    public void destroy() {
        if (parallelExecutor != null && !parallelExecutor.isShutdown()) {
            log.info("关闭 parallelExecutor 线程池...");
            parallelExecutor.shutdown();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CareerPlanReport createReportTask(Long studentId, String targetJobName) {
        StudentInfo student = studentInfoService.getById(studentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }

        // 删除该学生的所有旧报告（一个用户只能有一份报告）
        remove(new QueryWrapper<CareerPlanReport>().eq("student_id", studentId));
        log.info("已删除学生 {} 的旧报告，准备生成新报告", studentId);

        // 1. 创建初始报告记录（状态：处理中）
        CareerPlanReport report = new CareerPlanReport();
        report.setStudentId(studentId);
        report.setTargetJobId(0L);
        report.setTargetJobName(targetJobName.trim());
        report.setReportContent("");
        report.setActionPlan("{}");
        report.setStatus("PROCESSING");
        report.setProgress(0);
        report.setCurrentStep(1);
        report.setStepName(STEP_INIT);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        save(report);

        // 2. 异步执行报告生成
        // 使用 TransactionSynchronization 确保事务提交后再启动异步任务，
        // 避免异步线程在事务提交前查询数据库导致 getById 返回 null
        final Long reportId = report.getId();
        final Long studentIdFinal = studentId;
        final String jobNameFinal = targetJobName.trim();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                taskExecutor.execute(() -> executeReportGeneration(reportId, studentIdFinal, jobNameFinal));
            }
        });

        log.info("报告任务已创建: reportId={}, studentId={}, targetJob={}", reportId, studentId, targetJobName);
        return report;
    }

    /**
     * 异步执行报告生成（后台运行，不阻塞前端）
     */
    private void executeReportGeneration(Long reportId, Long studentId, String targetJobName) {
        long startTaskTime = System.currentTimeMillis();
        log.info("[ReportTask-{}] 异步任务开始执行", reportId);

        try {
            // 更新进度：获取学生信息
            updateProgress(reportId, 5, 1, STEP_STUDENT_PROFILE);

            StudentInfo student = studentInfoService.getById(studentId);
            if (student == null) {
                log.error("[ReportTask-{}] 学生不存在: studentId={}", reportId, studentId);
                markFailed(reportId, "学生不存在");
                return;
            }
            log.info("[ReportTask-{}] 学生信息获取成功: {}", reportId, student.getName());

            JobBasicInfo job = jobBasicInfoService.getOne(new QueryWrapper<JobBasicInfo>()
                    .eq("job_name", targetJobName)
                    .last("LIMIT 1"));

            if (job != null) {
                updateReportField(reportId, "target_job_id", job.getId());
                log.info("[ReportTask-{}] 岗位信息获取成功: {}", reportId, job.getJobName());
            } else {
                log.warn("[ReportTask-{}] 岗位信息未找到: {}", reportId, targetJobName);
            }

            String studentInfoJson = JSON.toJSONString(student);

            // 更新进度：生成学生画像
            updateProgress(reportId, 15, 1, STEP_STUDENT_PROFILE);
            log.info("[ReportTask-{}] 开始生成学生画像...", reportId);

            // 3. 获取或生成学生画像
            String studentProfileJson = studentAbilityProfileService.getLatestProfileJson(studentId);
            if (StringUtil.isBlank(studentProfileJson)) {
                try {
                    log.info("[ReportTask-{}] 未找到已有画像，开始AI生成...", reportId);
                    studentProfileJson = aiService.generateStudentProfile(student);
                    log.info("[ReportTask-{}] 学生画像AI生成成功，长度: {}", reportId, studentProfileJson.length());
                } catch (Exception e) {
                    log.warn("[ReportTask-{}] 生成学生画像失败，使用兜底画像: {}", reportId, e.getMessage());
                    studentProfileJson = FALLBACK_STUDENT_PROFILE;
                }
                try {
                    studentAbilityProfileService.saveProfileFromJson(studentId, studentProfileJson);
                } catch (Exception e) {
                    log.warn("[ReportTask-{}] 学生画像入库失败（继续生成报告）: {}", reportId, e.getMessage());
                }
            } else {
                log.info("[ReportTask-{}] 复用已有画像，长度: {}", reportId, studentProfileJson.length());
            }
            updateReportField(reportId, "student_profile_json", studentProfileJson);
            updateProgress(reportId, 30, 2, STEP_JOB_PROFILE);

            // 4. 生成岗位画像
            log.info("[ReportTask-{}] 开始生成岗位画像: {}", reportId, targetJobName);
            String jobProfileJson = generateJobProfileWithFallback(targetJobName);
            updateReportField(reportId, "job_profile_json", jobProfileJson);
            log.info("[ReportTask-{}] 岗位画像生成完毕，长度: {}", reportId, jobProfileJson.length());
            updateProgress(reportId, 45, 2, STEP_MATCH_ANALYSIS);

            // 5. 生成人岗匹配分析
            log.info("[ReportTask-{}] 开始生成人岗匹配分析...", reportId);
            String matchAnalysisJson;
            try {
                matchAnalysisJson = aiService.generateMatchAnalysis(studentProfileJson, jobProfileJson);
                if (matchAnalysisJson == null) matchAnalysisJson = "";
                matchAnalysisJson = matchAnalysisJson.trim();
                if (!matchAnalysisJson.startsWith("{")) {
                    log.warn("[ReportTask-{}] 人岗匹配分析返回非JSON，已忽略结构化匹配上下文", reportId);
                    matchAnalysisJson = "";
                } else {
                    log.info("[ReportTask-{}] 人岗匹配分析生成成功，长度: {}", reportId, matchAnalysisJson.length());
                }
            } catch (Exception e) {
                log.warn("[ReportTask-{}] 人岗匹配分析生成失败，继续生成报告: {}", reportId, e.getMessage());
                matchAnalysisJson = "";
            }
            updateReportField(reportId, "match_analysis_json", matchAnalysisJson);
            updateProgress(reportId, 55, 3, STEP_KNOWLEDGE);

            // 6. 获取知识库上下文（复用公共方法：三次Qdrant并行检索）
            log.info("[ReportTask-{}] 开始并行检索知识库...", reportId);
            String combinedContext = fetchKnowledgeContextsCombined(targetJobName);

            // 构建完整报告上下文（复用公共方法）
            String fullContextForReport = buildReportContext(studentProfileJson, matchAnalysisJson, combinedContext);
            log.info("[ReportTask-{}] 完整上下文构建完毕，总长度: {}字符", reportId, fullContextForReport.length());

            // 并行调用报告生成（一次性生成完整报告，不再额外调用generateMatchAnalysis）
            long reportStart = System.currentTimeMillis();
            CompletableFuture<String> reportFuture = CompletableFuture.supplyAsync(
                    () -> aiService.generateCareerPlanningReport(studentInfoJson, jobProfileJson, fullContextForReport, targetJobName),
                    parallelExecutor
            );

            // 等待报告生成完成（捕获 InterruptedException 和 ExecutionException）
            String fullReportJson;
            try {
                fullReportJson = reportFuture.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("[ReportTask-{}] 报告生成被中断", reportId);
                markFailed(reportId, "报告生成被中断");
                return;
            } catch (java.util.concurrent.ExecutionException e) {
                log.error("[ReportTask-{}] 报告生成执行异常: {}", reportId, e.getMessage());
                markFailed(reportId, "报告生成异常: " + e.getMessage());
                return;
            }
            long reportElapsed = System.currentTimeMillis() - reportStart;
            log.info("[ReportTask-{}] 报告生成完成，耗时: {}ms，返回长度: {}字符",
                    reportId, reportElapsed, fullReportJson.length());

            // 优化：校验并一次性保存所有数据（减少数据库写操作次数）
            CareerPlanReport finalReport = getById(reportId);
            if (finalReport == null) {
                log.error("[ReportTask-{}] 报告记录不存在", reportId);
                return;
            }

            try {
                fullReportJson = ensureValidFullReportJson(fullReportJson);
            } catch (Exception e) {
                log.error("[ReportTask-{}] 报告校验失败: {}", reportId, e.getMessage());
                markFailed(reportId, "报告内容校验失败: " + e.getMessage());
                return;
            }

            // 优化：批量更新报告字段，减少数据库操作
            finalReport.setReportContent(fullReportJson);
            finalReport.setStatus("FINAL");
            finalReport.setProgress(100);
            finalReport.setCurrentStep(4);
            finalReport.setStepName(STEP_COMPLETE);
            finalReport.setUpdatedAt(LocalDateTime.now());
            extractAndSetScore(finalReport, fullReportJson, matchAnalysisJson);
            updateById(finalReport);  // 一次性更新所有字段
            log.info("[ReportTask-{}] 报告生成完成并已保存！status=FINAL (总耗时: {}ms)",
                    reportId, System.currentTimeMillis() - startTaskTime);

        } catch (Exception e) {
            log.error("[ReportTask-{}] 报告生成过程中发生未捕获异常: {}", reportId, e.getMessage(), e);
            markFailed(reportId, "报告生成异常: " + e.getMessage());
        }
    }

    /**
     * 统一标记任务失败
     */
    private void markFailed(Long reportId, String errorMessage) {
        try {
            CareerPlanReport report = getById(reportId);
            if (report != null) {
                report.setStatus("FAILED");
                report.setErrorMessage(errorMessage);
                report.setProgress(0);
                report.setStepName("生成失败");
                report.setUpdatedAt(LocalDateTime.now());
                updateById(report);
                log.info("[ReportTask-{}] 任务已标记为FAILED: {}", reportId, errorMessage);
            }
        } catch (Exception ex) {
            log.error("[ReportTask-{}] 标记失败状态时异常: {}", reportId, ex.getMessage());
        }
    }

    /**
     * 更新报告进度
     */
    private void updateProgress(Long reportId, Integer progress, Integer currentStep, String stepName) {
        try {
            CareerPlanReport report = getById(reportId);
            if (report != null) {
                report.setProgress(progress);
                report.setCurrentStep(currentStep);
                report.setStepName(stepName);
                report.setUpdatedAt(LocalDateTime.now());
                updateById(report);
            }
        } catch (Exception e) {
            log.warn("更新进度失败: reportId={}, {}", reportId, e.getMessage());
        }
    }

    /**
     * 更新报告字段（动态字段映射，支持未来扩展）
     */
    private void updateReportField(Long reportId, String field, Object value) {
        try {
            CareerPlanReport report = getById(reportId);
            if (report == null) return;

            Map<String, java.util.function.BiConsumer<CareerPlanReport, Object>> setters = Map.of(
                "target_job_id", (r, v) -> { if (v instanceof Long) r.setTargetJobId((Long) v); },
                "student_profile_json", (r, v) -> { if (v instanceof String) r.setStudentProfileJson((String) v); },
                "job_profile_json", (r, v) -> { if (v instanceof String) r.setJobProfileJson((String) v); },
                "match_analysis_json", (r, v) -> { if (v instanceof String) r.setMatchAnalysisJson((String) v); },
                "progress", (r, v) -> { if (v instanceof Integer) r.setProgress((Integer) v); },
                "current_step", (r, v) -> { if (v instanceof Integer) r.setCurrentStep((Integer) v); },
                "step_name", (r, v) -> { if (v instanceof String) r.setStepName((String) v); },
                "error_message", (r, v) -> { if (v instanceof String) r.setErrorMessage((String) v); }
            );

            java.util.function.BiConsumer<CareerPlanReport, Object> setter = setters.get(field);
            if (setter != null) {
                setter.accept(report, value);
                report.setUpdatedAt(LocalDateTime.now());
                updateById(report);
            }
        } catch (Exception e) {
            log.warn("更新报告字段失败: reportId={}, field={}, {}", reportId, field, e.getMessage());
        }
    }

    /**
     * 同步生成报告（保持原有接口兼容）
     * 内部复用异步方法的核心逻辑，避免代码重复
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CareerPlanReport generateReport(Long studentId, String targetJobName) {
        long startTime = System.currentTimeMillis();
        log.info("开始同步生成报告: studentId={}, targetJob={}", studentId, targetJobName);

        StudentInfo student = studentInfoService.getById(studentId);
        if (student == null) {
            throw new BusinessException("学生不存在");
        }

        JobBasicInfo job = jobBasicInfoService.getOne(new QueryWrapper<JobBasicInfo>()
                .eq("job_name", targetJobName)
                .last("LIMIT 1"));

        String studentInfoJson = JSON.toJSONString(student);

        // 获取或生成学生画像
        String studentProfileJson = studentAbilityProfileService.getLatestProfileJson(studentId);
        if (StringUtil.isBlank(studentProfileJson)) {
            studentProfileJson = aiService.generateStudentProfile(student);
            try {
                studentAbilityProfileService.saveProfileFromJson(studentId, studentProfileJson);
            } catch (Exception e) {
                log.warn("学生画像入库失败（继续生成报告）: {}", e.getMessage());
            }
        }

        // 生成岗位画像
        String jobProfileJson = generateJobProfileWithFallback(targetJobName);

        // 生成人岗匹配分析（复用公共方法）
        String matchAnalysisJson = generateMatchAnalysisWithFallback(studentProfileJson, jobProfileJson);

        // 获取知识库上下文（复用公共方法）
        String combinedContext = fetchKnowledgeContextsCombined(targetJobName);

        // 构建报告上下文（复用公共方法）
        String fullContextForReport = buildReportContext(studentProfileJson, matchAnalysisJson, combinedContext);

        // 生成完整报告
        String fullReportJson = aiService.generateCareerPlanningReport(studentInfoJson, jobProfileJson, fullContextForReport, targetJobName);
        fullReportJson = ensureValidFullReportJson(fullReportJson);

        // 保存报告
        CareerPlanReport report = new CareerPlanReport();
        report.setStudentId(studentId);
        report.setTargetJobId(job != null ? job.getId() : 0L);
        report.setTargetJobName(targetJobName.trim());
        report.setReportContent(fullReportJson);
        report.setActionPlan("{}");
        report.setStatus("FINAL");
        report.setProgress(100);
        report.setCurrentStep(4);
        report.setStepName(STEP_COMPLETE);
        report.setStudentProfileJson(studentProfileJson);
        report.setJobProfileJson(jobProfileJson);
        report.setMatchAnalysisJson(matchAnalysisJson);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        extractAndSetScore(report, fullReportJson, matchAnalysisJson);
        remove(new QueryWrapper<CareerPlanReport>().eq("student_id", studentId));
        save(report);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("同步报告生成完成: reportId={}, 耗时{}ms", report.getId(), elapsed);

        return report;
    }

    @Override
    public CareerPlanReport getLatestReport(Long studentId) {
        return getOne(new QueryWrapper<CareerPlanReport>()
                .eq("student_id", studentId)
                .orderByDesc("created_at")
                .last("LIMIT 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CareerPlanReport updateReportContent(Long reportId, Long studentId, String reportContent) {
        if (reportId == null || reportId <= 0) {
            throw new BusinessException("报告ID无效");
        }
        if (studentId == null || studentId <= 0) {
            throw new BusinessException("学生ID无效");
        }
        if (reportContent == null || reportContent.trim().isEmpty()) {
            throw new BusinessException("报告内容不能为空");
        }

        CareerPlanReport report = getById(reportId);
        if (report == null) {
            throw new BusinessException("报告不存在");
        }
        if (!studentId.equals(report.getStudentId())) {
            throw new BusinessException("无权限更新该报告");
        }

        report.setReportContent(reportContent);
        report.setUpdatedAt(LocalDateTime.now());
        extractAndSetScore(report, reportContent, null);
        updateById(report);
        return report;
    }
    
    /**
     * 获取知识库上下文（双重兜底：Qdrant语义搜索 → KnowledgeVectorService → 空字符串）
     * 优先使用Qdrant的语义搜索能力，Qdrant不可用时fallback到KnowledgeVectorService
     */
    private String getKnowledgeContextWithFallback(String jobName) {
        // 1. 优先使用 QdrantRagService 的语义搜索（更智能的RAG能力）
        try {
            if (qdrantRagService.isServiceAvailable()) {
                String qdrantContext = qdrantRagService.getJobContext(jobName);
                if (StringUtil.isNotBlank(qdrantContext)) {
                    log.info("使用Qdrant RAG获取岗位上下文: {}", jobName);
                    return qdrantContext;
                }
            }
        } catch (Exception e) {
            log.warn("Qdrant RAG获取上下文失败，将使用备用方案: {}", e.getMessage());
        }

        // 2. Fallback到 KnowledgeVectorService（基于数据库的向量检索）
        try {
            String vectorContext = knowledgeVectorService.getJobContext(jobName);
            if (StringUtil.isNotBlank(vectorContext)) {
                log.info("Qdrant不可用，使用KnowledgeVectorService获取上下文: {}", jobName);
                return vectorContext;
            }
        } catch (Exception e) {
            log.warn("KnowledgeVectorService获取上下文失败: {}", e.getMessage());
        }

        // 3. 最后的兜底：使用KnowledgeBaseService
        try {
            String baseContext = knowledgeBaseService.getContextForReport(jobName);
            if (StringUtil.isNotBlank(baseContext)) {
                log.info("使用KnowledgeBaseService获取上下文: {}", jobName);
                return baseContext;
            }
        } catch (Exception e) {
            log.warn("KnowledgeBaseService获取上下文失败: {}", e.getMessage());
        }

        log.warn("所有知识库服务均不可用或无数据: {}", jobName);
        return "";
    }

    /**
     * 获取职业发展路径上下文（双重兜底）
     */
    private String getCareerPathContextWithFallback(String jobName) {
        // 1. 优先使用 QdrantRagService
        try {
            if (qdrantRagService.isServiceAvailable()) {
                String qdrantContext = qdrantRagService.getCareerPathContext(jobName);
                if (StringUtil.isNotBlank(qdrantContext)) {
                    return qdrantContext;
                }
            }
        } catch (Exception e) {
            log.warn("Qdrant RAG获取职业路径失败: {}", e.getMessage());
        }

        // 2. Fallback到数据库向量服务
        try {
            return knowledgeVectorService.getByJobNameAndType(jobName, "career_path")
                    .stream()
                    .map(v -> "### [" + v.getVectorType() + "] " + v.getJobName() + "\n" + v.getContent())
                    .reduce((a, b) -> a + "\n\n" + b)
                    .orElse("");
        } catch (Exception e) {
            log.warn("KnowledgeVectorService获取职业路径失败: {}", e.getMessage());
        }

        return "";
    }

    /**
     * 获取换岗路径上下文（双重兜底）
     */
    private String getTransferPathContextWithFallback(String jobName) {
        // 1. 优先使用 QdrantRagService
        try {
            if (qdrantRagService.isServiceAvailable()) {
                String qdrantContext = qdrantRagService.getTransferPathContext(jobName);
                if (StringUtil.isNotBlank(qdrantContext)) {
                    return qdrantContext;
                }
            }
        } catch (Exception e) {
            log.warn("Qdrant RAG获取换岗路径失败: {}", e.getMessage());
        }

        // 2. Fallback到数据库向量服务
        try {
            return knowledgeVectorService.getByJobNameAndType(jobName, "transfer_path")
                    .stream()
                    .map(v -> "### [" + v.getVectorType() + "] " + v.getJobName() + "\n" + v.getContent())
                    .reduce((a, b) -> a + "\n\n" + b)
                    .orElse("");
        } catch (Exception e) {
            log.warn("KnowledgeVectorService获取换岗路径失败: {}", e.getMessage());
        }

        return "";
    }

    private String generateJobProfileWithFallback(String jobName) {
        try {
            String result = jobBasicInfoService.generateAggregatedProfile(jobName);
            // 严格判断返回值：非null、非空、不包含"无数据"、且是有效JSON才使用
            if (result != null && !result.trim().isEmpty()
                    && !result.contains("无数据")
                    && isValidJson(result)) {
                return result;
            }
        } catch (Exception e) {
            log.warn("岗位画像生成失败，使用默认画像: {}", e.getMessage());
        }
        
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
                "experience": {"years": "1-3年", "type": "相关经验优先"},
                "soft_skills": {
                    "innovation_ability": {"level": 3, "description": "具备创新能力"},
                    "learning_ability": {"level": 4, "description": "良好的学习能力"},
                    "pressure_resistance": {"level": 3, "description": "能承受工作压力"},
                    "communication_ability": {"level": 4, "description": "良好的沟通能力"}
                },
                "career_path": {
                    "entry_level": "%s助理/初级",
                    "mid_level": "中级%s",
                    "senior_level": "高级%s/专家"
                },
                "salary_reference": "根据市场行情",
                "industry_outlook": "行业发展稳定"
            }
            """, jobName, jobName, jobName, jobName);
    }
    
    private boolean isValidJson(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        try {
            JSON.parseObject(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void extractAndSetScore(CareerPlanReport report, String fullReportJson, String matchAnalysisJson) {
        try {
            JSONObject json = JSON.parseObject(fullReportJson);
            
            if (json.containsKey("section_a")) {
                JSONObject sectionA = json.getJSONObject("section_a");
                if (sectionA.containsKey("overall_match")) {
                    JSONObject overallMatch = sectionA.getJSONObject("overall_match");
                    if (overallMatch.containsKey("total_score")) {
                        report.setMatchScore(BigDecimal.valueOf(overallMatch.getDoubleValue("total_score")));
                        return;
                    }
                }
            }

            if (matchAnalysisJson != null && !matchAnalysisJson.trim().isEmpty()) {
                JSONObject match = JSON.parseObject(matchAnalysisJson);
                if (match.containsKey("match_summary")) {
                    JSONObject summary = match.getJSONObject("match_summary");
                    if (summary.containsKey("total_score")) {
                        report.setMatchScore(BigDecimal.valueOf(summary.getDoubleValue("total_score")));
                        return;
                    }
                }
            }
            
            report.setMatchScore(null);
            return;
        } catch (Exception e) {
            log.warn("解析匹配分数失败，无法计算匹配度评分");
            report.setMatchScore(null);
        }
    }

    /**
     * 生成人岗匹配分析（带回退处理）
     * 提取为公共方法，供同步和异步报告生成共用
     */
    private String generateMatchAnalysisWithFallback(String studentProfileJson, String jobProfileJson) {
        String matchAnalysisJson;
        try {
            matchAnalysisJson = aiService.generateMatchAnalysis(studentProfileJson, jobProfileJson);
            if (matchAnalysisJson == null) matchAnalysisJson = "";
            matchAnalysisJson = matchAnalysisJson.trim();
            if (!matchAnalysisJson.startsWith("{")) {
                log.warn("人岗匹配分析返回非JSON，已忽略结构化匹配上下文");
                matchAnalysisJson = "";
            }
        } catch (Exception e) {
            log.warn("人岗匹配分析生成失败，继续生成报告: {}", e.getMessage());
            matchAnalysisJson = "";
        }
        return matchAnalysisJson;
    }

    /**
     * 并行获取知识库上下文（知识、晋升、换岗三条并行检索）
     * 提取为公共方法，供同步和异步报告生成共用
     * @return 合并后的知识库上下文字符串（可能为null）
     */
    private String fetchKnowledgeContextsCombined(String targetJobName) {
        long qdrantStart = System.currentTimeMillis();

        CompletableFuture<String> knowledgeFuture = CompletableFuture.supplyAsync(
                () -> getKnowledgeContextWithFallback(targetJobName), parallelExecutor);
        CompletableFuture<String> promotionFuture = CompletableFuture.supplyAsync(
                () -> getCareerPathContextWithFallback(targetJobName), parallelExecutor);
        CompletableFuture<String> transferFuture = CompletableFuture.supplyAsync(
                () -> getTransferPathContextWithFallback(targetJobName), parallelExecutor);

        String knowledgeContext = "";
        String promotionContext = "";
        String transferContext = "";
        try {
            CompletableFuture.allOf(knowledgeFuture, promotionFuture, transferFuture).join();
            knowledgeContext = knowledgeFuture.get();
            promotionContext = promotionFuture.get();
            transferContext = transferFuture.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("知识库并行检索被中断，使用空上下文");
        } catch (java.util.concurrent.ExecutionException e) {
            log.warn("知识库并行检索执行异常: {}", e.getMessage());
            try {
                knowledgeContext = knowledgeFuture.isDone() ? knowledgeFuture.get() : "";
                promotionContext = promotionFuture.isDone() ? promotionFuture.get() : "";
                transferContext = transferFuture.isDone() ? transferFuture.get() : "";
            } catch (Exception ex) {
                // 使用空字符串默认值
            }
        }

        long qdrantElapsed = System.currentTimeMillis() - qdrantStart;
        log.info("知识库并行检索完成，耗时: {}ms (知识={}字符, 晋升={}字符, 换岗={}字符)",
                qdrantElapsed,
                knowledgeContext != null ? knowledgeContext.length() : 0,
                promotionContext != null ? promotionContext.length() : 0,
                transferContext != null ? transferContext.length() : 0);

        StringBuilder fullContext = new StringBuilder();
        if (StringUtil.isNotBlank(knowledgeContext)) {
            fullContext.append(knowledgeContext);
        }
        if (StringUtil.isNotBlank(promotionContext)) {
            fullContext.append("\n\n").append(promotionContext);
        }
        if (StringUtil.isNotBlank(transferContext)) {
            fullContext.append("\n\n").append(transferContext);
        }
        return fullContext.length() > 0 ? fullContext.toString() : null;
    }

    /**
     * 构建传给报告生成器的完整上下文
     * 提取为公共方法，供同步和异步报告生成共用
     */
    private String buildReportContext(String studentProfileJson, String matchAnalysisJson, String combinedContext) {
        StringBuilder reportContext = new StringBuilder();
        reportContext.append("# 学生就业能力画像（结构化）\n").append(studentProfileJson).append("\n\n");
        if (StringUtil.isNotBlank(matchAnalysisJson)) {
            reportContext.append("# 人岗匹配四维分析（结构化）\n").append(matchAnalysisJson).append("\n\n");
        }
        if (StringUtil.isNotBlank(combinedContext)) {
            reportContext.append(combinedContext);
        }
        return reportContext.toString();
    }

    /**
     * 严格校验报告JSON，避免将空/异常内容误存为成功状态
     * @return 清理后的JSON字符串（用于后续保存）
     */
    private String ensureValidFullReportJson(String fullReportJson) {
        if (StringUtil.isBlank(fullReportJson)) {
            throw new BusinessException("AI返回报告内容为空");
        }

        // 防御性清理：去掉可能的思考标签（第二层保护，extractJson是第一层）
        String cleaned = fullReportJson
            .replaceAll("(?s)<\\|reserved_think\\|>[\\s\\S]*?<\\|reserved_think\\|>", "")
            .replaceAll("(?s)<\\|thinking\\|>[\\s\\S]*?<\\|thinking\\|>", "")
            .replaceAll("(?s)<\\|im_start\\|>think[\\s\\S]*?<\\|im_end\\|>", "")
            .replaceAll("(?s)<\\|im_start\\|>result[\\s\\S]*?<\\|im_end\\|>", "")
            .replaceAll("(?s)<[^>]+>[\\s\\S]*?</[^>]+>", "") // 去掉所有HTML/XML标签对
            .trim();

        if (StringUtil.isBlank(cleaned)) {
            throw new BusinessException("AI返回报告内容为空（清理思考标签后为空）");
        }

        try {
            JSONObject json = JSON.parseObject(cleaned);
            if (json == null || json.isEmpty()) {
                log.error("报告JSON解析结果为空对象，原文前200字符: {}", truncate(cleaned));
                throw new BusinessException("AI返回报告内容为空对象");
            }
            boolean hasCoreSection = json.containsKey("section_a")
                    || json.containsKey("section_b")
                    || json.containsKey("section_c")
                    || json.containsKey("section_d");
            if (!hasCoreSection) {
                log.error("报告缺少核心章节，keys: {}", json.keySet());
                throw new BusinessException("AI返回报告结构异常，缺少核心章节");
            }

            // 兜底：当 gap_analysis 缺失时，从 gap_skills / skill_details 自动推断补全
            fillMissingGapAnalysis(json);

            log.info("报告JSON校验通过，包含章节: {}", json.keySet());
            // 返回序列化后的JSON字符串（确保字段顺序和格式正确）
            return JSON.toJSONString(json);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("报告JSON解析异常，原文前200字符: {}, 错误: {}", truncate(fullReportJson), e.getMessage());
            throw new BusinessException("AI返回报告JSON格式异常: " + e.getMessage());
        }
    }

    /**
     * 兜底：当 gap_analysis 缺失或为空时，从 gap_skills / skill_details 自动推断补全
     * 解决AI偶发遗漏该字段导致前端差距分析区块不显示的问题
     */
    private void fillMissingGapAnalysis(JSONObject json) {
        JSONObject sectionA = json.getJSONObject("section_a");
        if (sectionA == null) {
            log.warn("[GapFill] section_a 不存在，跳过差距分析兜底");
            return;
        }

        JSONObject gapAnalysis = sectionA.getJSONObject("gap_analysis");
        log.info("[GapFill] 当前 gap_analysis = {}", gapAnalysis);

        boolean needsFill = false;

        if (gapAnalysis == null) {
            log.info("[GapFill] gap_analysis 为 null，需要填充");
            gapAnalysis = new JSONObject();
            needsFill = true;
        } else {
            // 检查三个分类是否都为空
            JSONArray critical = gapAnalysis.getJSONArray("critical_gaps");
            JSONArray moderate = gapAnalysis.getJSONArray("moderate_gaps");
            JSONArray minor = gapAnalysis.getJSONArray("minor_gaps");
            boolean criticalEmpty = critical == null || critical.isEmpty();
            boolean moderateEmpty = moderate == null || moderate.isEmpty();
            boolean minorEmpty = minor == null || minor.isEmpty();
            if (criticalEmpty && moderateEmpty && minorEmpty) {
                log.info("[GapFill] 三个分类全部为空，需要填充");
                needsFill = true;
                gapAnalysis = new JSONObject();
            }
        }

        if (!needsFill) {
            log.info("[GapFill] gap_analysis 已存在且有内容，跳过兜底");
            return;
        }

        log.info("[GapFill] 开始从 gap_skills / skill_details 自动推断...");

        // 优先从 professional_skill_match.gap_skills 推断
        JSONObject psm = sectionA.getJSONObject("professional_skill_match");
        JSONArray gapSkills = psm != null ? psm.getJSONArray("gap_skills") : null;
        JSONArray skillDetails = psm != null ? psm.getJSONArray("skill_details") : null;

        JSONArray criticalGaps = new JSONArray();
        JSONArray moderateGaps = new JSONArray();
        JSONArray minorGaps = new JSONArray();

        // 策略：从 gap_skills 按优先级字段推断
        if (gapSkills != null && !gapSkills.isEmpty()) {
            for (int i = 0; i < gapSkills.size(); i++) {
                Object item = gapSkills.get(i);
                if (!(item instanceof JSONObject)) {
                    if (item instanceof String) {
                        JSONObject gapItem = new JSONObject();
                        gapItem.put("skill", item.toString());
                        gapItem.put("reason", "岗位要求但尚未掌握，需针对性学习");
                        gapItem.put("priority", "高");
                        criticalGaps.add(gapItem);
                    }
                    continue;
                }

                JSONObject obj = (JSONObject) item;
                String skillName = obj.getString("name");
                if (StringUtil.isBlank(skillName)) continue;

                String priority = obj.getString("priority");
                String reason = obj.getString("reason");
                String learningPath = obj.getString("learning_path");
                String suggestedDuration = obj.getString("suggested_duration");
                int difficulty = obj.getIntValue("difficulty");

                if (StringUtil.isBlank(reason)) {
                    reason = StringUtil.isNotBlank(learningPath)
                        ? learningPath
                        : "岗位要求但尚未掌握，需针对性学习";
                }

                JSONObject gapItem = new JSONObject();
                gapItem.put("skill", skillName);
                gapItem.put("reason", reason);
                gapItem.put("priority", priority);
                gapItem.put("difficulty", difficulty);
                gapItem.put("suggested_duration", suggestedDuration);
                gapItem.put("learning_path", learningPath);

                if ("高".equals(priority) || "紧急".equals(priority)) {
                    criticalGaps.add(gapItem);
                } else if ("中".equals(priority)) {
                    moderateGaps.add(gapItem);
                } else {
                    minorGaps.add(gapItem);
                }
            }
        }

        // 策略：从 skill_details 中 gap="较大"/"大" 的项作为关键差距兜底补充
        if (skillDetails != null && !skillDetails.isEmpty()) {
            for (int i = 0; i < skillDetails.size(); i++) {
                JSONObject detail = skillDetails.getJSONObject(i);
                if (detail == null) continue;

                String skill = detail.getString("skill");
                String gap = detail.getString("gap");
                String suggestion = detail.getString("suggestion");
                String requiredLevel = detail.getString("required_level");
                String studentLevel = detail.getString("student_level");
                String gapDescription = detail.getString("gap_description");

                if (StringUtil.isBlank(skill)) continue;

                // 检查是否已在 criticalGaps 中（避免重复）
                boolean alreadyAdded = false;
                for (int j = 0; j < criticalGaps.size(); j++) {
                    JSONObject c = criticalGaps.getJSONObject(j);
                    if (c != null && skill.equals(c.getString("skill"))) {
                        alreadyAdded = true;
                        break;
                    }
                }
                for (int j = 0; j < moderateGaps.size(); j++) {
                    JSONObject m = moderateGaps.getJSONObject(j);
                    if (m != null && skill.equals(m.getString("skill"))) {
                        alreadyAdded = true;
                        break;
                    }
                }
                if (alreadyAdded) continue;

                // 根据 gap 字段推断差距等级
                if (gap != null && (gap.contains("大") || gap.contains("不足") || gap.contains("较大"))) {
                    JSONObject gapItem = new JSONObject();
                    gapItem.put("skill", skill);
                    gapItem.put("required_level", requiredLevel);
                    gapItem.put("student_level", studentLevel);
                    gapItem.put("gap_severity", "较大");
                    gapItem.put("reason", StringUtil.isNotBlank(gapDescription) ? gapDescription : (StringUtil.isNotBlank(suggestion) ? suggestion : "与岗位要求存在明显差距"));
                    gapItem.put("suggestion", suggestion);
                    gapItem.put("priority", "紧急");
                    criticalGaps.add(gapItem);
                } else if (gap != null && gap.contains("中")) {
                    JSONObject gapItem = new JSONObject();
                    gapItem.put("skill", skill);
                    gapItem.put("required_level", requiredLevel);
                    gapItem.put("student_level", studentLevel);
                    gapItem.put("gap_severity", "一般");
                    gapItem.put("reason", StringUtil.isNotBlank(suggestion) ? suggestion : "有一定差距，建议加强");
                    gapItem.put("suggestion", suggestion);
                    gapItem.put("priority", "中等");
                    moderateGaps.add(gapItem);
                } else if (gap != null && gap.contains("小") && minorGaps.size() < 3) {
                    JSONObject gapItem = new JSONObject();
                    gapItem.put("skill", skill);
                    gapItem.put("reason", StringUtil.isNotBlank(suggestion) ? suggestion : "小差距，可后续优化");
                    gapItem.put("suggestion", suggestion);
                    gapItem.put("priority", "低");
                    minorGaps.add(gapItem);
                }
            }
        }

        // 如果仍然为空，添加一条通用提示
        if (criticalGaps.isEmpty() && moderateGaps.isEmpty() && minorGaps.isEmpty()) {
            log.warn("[GapFill] gap_skills 和 skill_details 也为空，无法推断差距分析，仅添加通用占位");
            JSONObject placeholder = new JSONObject();
            placeholder.put("skill", "专业技能差距");
            placeholder.put("reason", "岗位要求与个人能力存在差距，建议参考上方技能详细对比表进行针对性提升");
            criticalGaps.add(placeholder);
        }

        gapAnalysis.put("critical_gaps", criticalGaps);
        gapAnalysis.put("moderate_gaps", moderateGaps);
        gapAnalysis.put("minor_gaps", minorGaps);
        sectionA.put("gap_analysis", gapAnalysis);

        log.info("[GapFill] 兜底填充完成：关键差距={}项, 中等差距={}项, 轻微差距={}项",
                criticalGaps.size(), moderateGaps.size(), minorGaps.size());
    }

    private String truncate(String str) {
        if (str == null) return "null";
        if (str.length() <= 200) return str;
        return str.substring(0, 200) + "...";
    }
}
