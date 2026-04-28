package com.career.planning.controller;

import com.career.planning.common.CommonResult;
import com.career.planning.common.ProfileConstants;
import com.career.planning.config.OssConfig;
import com.career.planning.entity.StudentInfo;
import com.career.planning.service.AIService;
import com.career.planning.service.OssService;
import com.career.planning.service.StudentAbilityProfileService;
import com.career.planning.service.StudentInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PreDestroy;

@RestController
@RequestMapping("/api/student")
public class StudentInfoController {

    private static final Logger log = LoggerFactory.getLogger(StudentInfoController.class);

    /**
     * 画像生成进度缓存
     * key: studentId
     * value: {progress, stepName, completed, profile}
     *
     * 注意：使用 ConcurrentHashMap + 大小限制，防止内存泄漏
     * 已完成/失败的条目会在一定时间后被清理
     */
    private static final int MAX_CACHE_SIZE = 500;
    private static final ConcurrentHashMap<Long, ProfileGenerationContext> profileProgressCache = new ConcurrentHashMap<>();

    /**
     * 进度上下文
     */
    private static class ProfileGenerationContext {
        volatile int progress = 0;
        volatile String stepName = "等待初始化...";
        volatile boolean completed = false;
        volatile boolean failed = false;
        volatile String profile = null;

        void update(int progress, String stepName) {
            this.progress = progress;
            this.stepName = stepName;
        }

        void complete(String profile) {
            this.completed = true;
            this.progress = 100;
            this.stepName = "画像生成完成";
            this.profile = profile;
            this.completedAt = System.currentTimeMillis();
        }

        void fail() {
            this.failed = true;
            this.progress = 0;
            this.stepName = "生成失败";
            this.completedAt = System.currentTimeMillis();
        }

        /** 记录完成/失败的时间戳，用于过期清理 */
        volatile long completedAt = 0;
    }

    /**
     * Bean销毁时清理缓存，防止内存泄漏
     */
    @PreDestroy
    public void cleanupProfileCache() {
        log.info("清理画像进度缓存，当前大小: {}", profileProgressCache.size());
        profileProgressCache.clear();
    }

    /**
     * 清理已完成/失败超过5分钟的条目，防止内存无限增长
     * 在每次创建新任务前调用
     */
    private void evictStaleEntries() {
        long now = System.currentTimeMillis();
        long staleThresholdMs = 5 * 60 * 1000; // 5分钟
        profileProgressCache.entrySet().removeIf(entry -> {
            ProfileGenerationContext ctx = entry.getValue();
            return (ctx.completed || ctx.failed) && ctx.completedAt > 0 && (now - ctx.completedAt) > staleThresholdMs;
        });
        // 如果缓存过大，清理最早的条目
        if (profileProgressCache.size() > MAX_CACHE_SIZE) {
            log.warn("画像进度缓存超过 {} 条，执行强制清理", MAX_CACHE_SIZE);
            profileProgressCache.clear();
        }
    }

    @Value("${upload.base-path:}")
    private String uploadBasePath;

    @Value("${upload.resumes-subdir:resumes}")
    private String resumesSubdir;

    @Autowired
    private StudentInfoService studentInfoService;
    
    @Autowired
    private AIService aiService;

    @Autowired
    private StudentAbilityProfileService studentAbilityProfileService;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired(required = false)
    private OssService ossService;

    @Autowired(required = false)
    private OssConfig ossConfig;

    @PutMapping("/{id}")
    public CommonResult<String> updateInfo(@PathVariable Long id, @RequestBody StudentInfo info) {
        log.info("收到更新请求: id={}, info={}", id, info);
        StudentInfo student = studentInfoService.getById(id);
        if (student == null) {
            log.error("更新失败: 学生ID {} 不存在", id);
            return CommonResult.failed("学生不存在");
        }
        
        // 构造更新对象（仅更新有实际内容的字段，避免空字符串覆盖数据库原有值）
        if (info.getName() != null && !info.getName().trim().isEmpty()) student.setName(info.getName().trim());
        if (info.getMajor() != null && !info.getMajor().trim().isEmpty()) student.setMajor(info.getMajor().trim());
        if (info.getGraduationSchool() != null && !info.getGraduationSchool().trim().isEmpty()) student.setGraduationSchool(info.getGraduationSchool().trim());
        if (info.getEducation() != null && !info.getEducation().trim().isEmpty()) student.setEducation(info.getEducation().trim());
        if (info.getGraduationYear() != null) student.setGraduationYear(info.getGraduationYear());
        if (info.getCareerIntention() != null && !info.getCareerIntention().trim().isEmpty()) student.setCareerIntention(info.getCareerIntention().trim());
        if (info.getPersonalityTraits() != null && !info.getPersonalityTraits().trim().isEmpty()) student.setPersonalityTraits(info.getPersonalityTraits().trim());
        if (info.getWorkExperienceYears() != null && !info.getWorkExperienceYears().trim().isEmpty()) student.setWorkExperienceYears(info.getWorkExperienceYears().trim());
        if (info.getResumeFilePath() != null && !info.getResumeFilePath().trim().isEmpty()) student.setResumeFilePath(info.getResumeFilePath().trim());

        student.setUpdatedAt(LocalDateTime.now());
        studentInfoService.updateById(student);
        
        return CommonResult.success("信息保存成功");
    }

    @PostMapping("/upload/resume")
    public CommonResult<String> uploadResume(@RequestParam("file") MultipartFile file, @RequestParam("studentId") Long studentId) {
        if (file.isEmpty()) {
            return CommonResult.failed("文件为空");
        }

        try {
            StudentInfo student = studentInfoService.getById(studentId);
            if (student == null) {
                return CommonResult.failed("学生不存在");
            }

            // 先删除旧的OSS简历文件（如果有）
            String oldResumeUrl = student.getResumeFilePath();
            if (isOssEnabled() && StringUtils.hasText(oldResumeUrl)) {
                ossService.deleteFile(oldResumeUrl);
                log.info("已删除旧简历: {}", oldResumeUrl);
            }

            String fileUrl;

            if (isOssEnabled()) {
                fileUrl = uploadToOss(file);
                log.info("文件上传到OSS成功: {}", fileUrl);
            } else {
                fileUrl = uploadToLocal(file);
                log.info("文件上传到本地成功: {}", fileUrl);
            }

            student.setResumeFilePath(fileUrl);
            studentInfoService.updateById(student);

            return CommonResult.success(fileUrl);
        } catch (Exception e) {
            log.error("文件上传失败: ", e);
            return CommonResult.failed("文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/profile/create")
    public CommonResult<Map<String, Object>> createProfileTask(@PathVariable Long id) {
        log.info("收到创建个人画像任务请求: id={}", id);
        try {
            StudentInfo student = studentInfoService.getById(id);
            if (student == null) {
                log.error("创建失败: 学生ID {} 不存在", id);
                return CommonResult.failed("学生不存在");
            }
            if (!StringUtils.hasText(student.getName())
                    || !StringUtils.hasText(student.getGraduationSchool())
                    || !StringUtils.hasText(student.getMajor())
                    || !StringUtils.hasText(student.getEducation())
                    || student.getGraduationYear() == null) {
                return CommonResult.validateFailed(
                        "生成画像前请先在「个人信息」中保存完整教育信息：姓名、毕业院校、专业、最高学历、毕业年份");
            }
            log.info("学生信息: {}", student);

            // 清理过期的缓存条目
            evictStaleEntries();

            // 初始化进度上下文
            ProfileGenerationContext ctx = new ProfileGenerationContext();
            ctx.update(5, "正在初始化...");
            profileProgressCache.put(id, ctx);

            // 使用Spring管理的TaskExecutor异步执行
            Long studentId = id;
            taskExecutor.execute(() -> executeProfileGeneration(studentId));

            return CommonResult.success(Map.of(
                    "studentId", id,
                    "status", "PROCESSING",
                    "stepName", "正在初始化..."
            ));
        } catch (Exception e) {
            log.error("创建画像任务失败", e);
            return CommonResult.failed("任务创建失败: " + e.getMessage());
        }
    }

    /**
     * 异步执行个人画像生成
     */
    private void executeProfileGeneration(Long studentId) {
        ProfileGenerationContext ctx = profileProgressCache.get(studentId);
        try {
            StudentInfo student = studentInfoService.getById(studentId);
            if (student == null) {
                log.error("画像生成失败: 学生不存在 studentId={}", studentId);
                if (ctx != null) ctx.fail();
                return;
            }

            // 阶段1: 准备环境 (5%)
            if (ctx != null) ctx.update(5, "正在初始化...");

            // 阶段2: 读取学生信息 (15%)
            if (ctx != null) ctx.update(15, "正在读取个人信息...");

            // 阶段3: 读取并解析简历 (25%)
            if (ctx != null) ctx.update(25, "正在解析简历文件...");

            // 阶段4: AI分析专业技能 (35% -> 45%)
            // 进度回调会在这阶段内部触发：AI调用开始时更新到 35%，完成后进入下一阶段
            if (ctx != null) ctx.update(35, "AI正在分析专业技能...");

            String profile = aiService.generateStudentProfile(student, (progress, stepName) -> {
                // AI服务内部回调：简历解析完成后/AI开始分析时通知
                if (ctx != null) ctx.update(45, "AI正在分析专业技能...");
            });

            // 阶段5: AI分析软技能 (55%)
            if (ctx != null) ctx.update(55, "AI正在分析软技能...");

            // 阶段6: 生成综合评估 (75%)
            if (ctx != null) ctx.update(75, "AI正在生成综合评估...");

            // 阶段7: 保存结果 (90%)
            if (ctx != null) ctx.update(90, "正在保存画像结果...");

            studentAbilityProfileService.saveProfileFromJson(studentId, profile);

            // 阶段8: 完成 (100%)
            if (ctx != null) ctx.complete(profile);

            log.info("个人画像生成完成: studentId={}", studentId);

        } catch (Exception e) {
            log.error("个人画像生成异常: studentId={}, error={}", studentId, e.getMessage(), e);
            if (ctx != null) ctx.fail();
            try {
                // 保存兜底画像（使用集中管理的常量，避免重复定义）
                String fallbackProfile = ProfileConstants.FALLBACK_STUDENT_PROFILE;
                studentAbilityProfileService.saveProfileFromJson(studentId, fallbackProfile);
            } catch (Exception ex) {
                log.warn("兜底画像入库失败: {}", ex.getMessage());
            }
        }
    }

    /**
     * 轮询个人画像生成状态
     */
    @GetMapping("/{id}/profile/status")
    public CommonResult<Map<String, Object>> getProfileStatus(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return CommonResult.failed("学生ID无效");
        }
        try {
            // 优先从内存缓存中获取进度
            ProfileGenerationContext ctx = profileProgressCache.get(id);

            // 如果缓存中标记为完成，直接返回完成状态
            if (ctx != null && ctx.completed) {
                String profile = studentAbilityProfileService.getLatestProfileJson(id);
                profileProgressCache.remove(id);
                return CommonResult.success(Map.of(
                        "studentId", id,
                        "status", "COMPLETED",
                        "progress", 100,
                        "stepName", "画像生成完成",
                        "profile", profile != null ? profile : ctx.profile
                ));
            }

            // 如果缓存中标记为失败
            if (ctx != null && ctx.failed) {
                profileProgressCache.remove(id);
                return CommonResult.success(Map.of(
                        "studentId", id,
                        "status", "FAILED",
                        "progress", 0,
                        "stepName", "画像生成失败"
                ));
            }

            // 如果缓存中还在处理中，返回当前进度
            if (ctx != null) {
                return CommonResult.success(Map.of(
                        "studentId", id,
                        "status", "PROCESSING",
                        "progress", ctx.progress,
                        "stepName", ctx.stepName
                ));
            }

            // 缓存中没有记录，说明任务还未开始或已结束（首次轮询时可能还没创建缓存）
            // 此时尝试从数据库获取最新画像
            String latestProfile = studentAbilityProfileService.getLatestProfileJson(id);
            if (StringUtils.hasText(latestProfile)) {
                return CommonResult.success(Map.of(
                        "studentId", id,
                        "status", "COMPLETED",
                        "progress", 100,
                        "stepName", "画像生成完成",
                        "profile", latestProfile
                ));
            }

            // 数据库中也没有，返回等待状态
            return CommonResult.success(Map.of(
                    "studentId", id,
                    "status", "PENDING",
                    "progress", 0,
                    "stepName", "等待生成..."
            ));
        } catch (Exception e) {
            return CommonResult.failed("获取状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/profile/generate")
    public CommonResult<String> generateAbilityProfile(@PathVariable Long id) {
        log.info("收到生成画像请求: id={}", id);
        try {
            StudentInfo student = studentInfoService.getById(id);
            if (student == null) {
                log.error("生成失败: 学生ID {} 不存在", id);
                return CommonResult.failed("学生不存在");
            }
            if (!StringUtils.hasText(student.getName())
                    || !StringUtils.hasText(student.getGraduationSchool())
                    || !StringUtils.hasText(student.getMajor())
                    || !StringUtils.hasText(student.getEducation())
                    || student.getGraduationYear() == null) {
                return CommonResult.validateFailed(
                        "生成画像前请先在「个人信息」中保存完整教育信息：姓名、毕业院校、专业、最高学历、毕业年份");
            }
            log.info("学生信息: {}", student);
            
            String profile = aiService.generateStudentProfile(student);
            try {
                studentAbilityProfileService.saveProfileFromJson(id, profile);
            } catch (Exception ex) {
                log.warn("学生画像入库失败（不影响返回JSON）: {}", ex.getMessage());
            }
            return CommonResult.success(profile);
        } catch (Exception e) {
            log.error("生成画像发生未知异常", e);
            // 使用集中管理的兜底画像常量
            String fallbackProfile = ProfileConstants.FALLBACK_STUDENT_PROFILE;
            try {
                studentAbilityProfileService.saveProfileFromJson(id, fallbackProfile);
            } catch (Exception ex) {
                log.warn("兜底画像入库失败: {}", ex.getMessage());
            }
            return CommonResult.success(fallbackProfile);
        }
    }

    @GetMapping("/{id}/profile/latest")
    public CommonResult<String> getLatestAbilityProfile(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return CommonResult.failed("学生ID无效");
        }
        String latestProfile = studentAbilityProfileService.getLatestProfileJson(id);
        // 没有画像时返回 200 + null，让前端自行判断并展示引导页面
        if (!StringUtils.hasText(latestProfile)) {
            return CommonResult.success(null);
        }
        return CommonResult.success(latestProfile);
    }

    private boolean isOssEnabled() {
        return ossConfig != null 
                && StringUtils.hasText(ossConfig.getEndpoint())
                && StringUtils.hasText(ossConfig.getAccessKeyId())
                && StringUtils.hasText(ossConfig.getAccessKeySecret())
                && StringUtils.hasText(ossConfig.getBucketName())
                && ossService != null;
    }

    private String uploadToOss(MultipartFile file) {
        return ossService.uploadFile(file, "resumes");
    }

    /**
     * 上传文件到本地存储
     * 使用配置的路径，优先使用 upload.base-path，不配置时使用 user.dir/data/resumes
     */
    private String uploadToLocal(MultipartFile file) throws IOException {
        // 1. 生成安全的文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 限制文件扩展名
            if (!extension.matches("\\.(pdf|doc|docx|txt)$")) {
                extension = ".pdf"; // 默认pdf
            }
        }
        String safeFileName = UUID.randomUUID().toString() + extension;

        // 2. 构建路径 - 优先使用配置的 base-path，否则使用 user.dir
        Path dataDir;
        if (StringUtils.hasText(uploadBasePath)) {
            dataDir = Paths.get(uploadBasePath, resumesSubdir);
        } else {
            dataDir = Paths.get(System.getProperty("user.dir"), "data", resumesSubdir);
        }

        // 3. 创建目录（如果不存在）
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
            log.info("创建简历上传目录: {}", dataDir.toAbsolutePath());
        }

        // 4. 保存文件
        Path targetPath = dataDir.resolve(safeFileName);
        file.transferTo(targetPath.toFile());

        log.info("文件上传到本地成功: {}", targetPath.toAbsolutePath());
        return targetPath.toAbsolutePath().toString();
    }
}
