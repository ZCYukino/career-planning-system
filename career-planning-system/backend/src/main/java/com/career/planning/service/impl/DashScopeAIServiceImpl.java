package com.career.planning.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.career.planning.common.BusinessException;
import com.career.planning.common.FileParserUtils;
import com.career.planning.common.PromptTemplate;
import com.career.planning.config.DashScopeConfig;
import com.career.planning.entity.JobBasicInfo;
import com.career.planning.entity.StudentInfo;
import com.career.planning.service.AIService;
import com.career.planning.service.OssService;
import com.career.planning.util.StringUtil;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 阿里云百炼 AI 服务实现
 * 仅在 ai.provider=dashscope 时加载
 */
@Service
@ConditionalOnProperty(name = "ai.provider", havingValue = "dashscope")
public class DashScopeAIServiceImpl implements AIService {

    private static final Logger log = LoggerFactory.getLogger(DashScopeAIServiceImpl.class);

    private static final String CHAT_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    // 使用 qwen-turbo 轻量模型，响应速度快、成本低，足够处理结构化JSON生成任务
    private static final String CHAT_MODEL = "qwen-turbo";

    @Autowired
    private DashScopeConfig dashScopeConfig;

    @Autowired(required = false)
    private OssService ossService;

    /**
     * 简历文本最大截取长度（AI输入限制约8K字符，留出足够空间给其他提示词）
     */
    @Value("${ai.resume.max-text-length:4000}")
    private int resumeMaxTextLength;

    private static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * AI服务重试配置
     */
    @Value("${ai.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${ai.retry.backoff-ms:1000}")
    private long retryBackoffMs;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    @Override
    public String generateJobProfile(JobBasicInfo jobInfo) {
        String prompt = PromptTemplate.JOB_PROFILE_PROMPT
                .replace("{0}", jobInfo.getJobName())
                .replace("{1}", jobInfo.getJobDescription() != null ? jobInfo.getJobDescription() : "无")
                .replace("{2}", "无（使用通用推断）");
        return extractJson(callChat(prompt));
    }

    @Override
    public String generateJobProfileWithKnowledge(JobBasicInfo jobInfo, String knowledgeContext) {
        String ctx = (knowledgeContext != null && !knowledgeContext.isEmpty())
                ? knowledgeContext : "无（使用通用推断）";
        String prompt = PromptTemplate.JOB_PROFILE_PROMPT
                .replace("{0}", jobInfo.getJobName())
                .replace("{1}", jobInfo.getJobDescription() != null ? jobInfo.getJobDescription() : "无")
                .replace("{2}", ctx);
        return extractJson(callChat(prompt));
    }

    @Override
    public String generateStudentProfile(StudentInfo studentInfo) {
        return generateStudentProfile(studentInfo, null);
    }

    @Override
    public String generateStudentProfile(StudentInfo studentInfo, AIService.ProgressCallback callback) {
        // 阶段1: 解析简历文件 (进度由调用方在 Controller 层更新，这里只负责 AI 生成)
        String resumeContent = extractResumeTextForPrompt(studentInfo);

        String prompt = PromptTemplate.STUDENT_PROFILE_PROMPT
                .replace("{0}", textOrPlaceholder(studentInfo.getName()))
                .replace("{1}", textOrPlaceholder(studentInfo.getGraduationSchool()))
                .replace("{2}", textOrPlaceholder(studentInfo.getMajor()))
                .replace("{3}", textOrPlaceholder(studentInfo.getEducation()))
                .replace("{4}", graduationYearText(studentInfo.getGraduationYear()))
                .replace("{5}", textOrPlaceholder(studentInfo.getCareerIntention()))
                .replace("{6}", textOrPlaceholder(studentInfo.getPersonalityTraits()))
                .replace("{7}", textOrPlaceholder(studentInfo.getWorkExperienceYears()))
                .replace("{8}", resumeContent);

        // 阶段2: AI分析中 (回调通知，触发进度条从 25% -> 35%)
        if (callback != null) {
            callback.onProgress(35, "AI正在分析专业技能...");
        }

        // 阶段3: AI生成中 (等待网络请求完成，此阶段 UI 保持 35% 动态，等待轮询)
        return extractJson(callChat(prompt));
    }

    private static String textOrPlaceholder(String s) {
        return (s != null && !s.isBlank()) ? s : "未填写";
    }

    private static String graduationYearText(Integer year) {
        return year != null ? String.valueOf(year) : "未填写";
    }

    private String extractResumeTextForPrompt(StudentInfo studentInfo) {
        String path = studentInfo.getResumeFilePath();
        if (path == null || path.isEmpty()) {
            log.info("学生简历路径为空: studentId={}", studentInfo.getId());
            return "【简历信息】：未上传简历";
        }
        log.info("开始解析简历: path={}", path);

        // 判断是否为本地文件路径（绝对路径或相对路径）
        boolean isLocalPath = !path.startsWith("http://") && !path.startsWith("https://");

        // 优先尝试本地文件解析（本地存储场景）
        if (isLocalPath) {
            try {
                String resumeText = FileParserUtils.parseFile(path);
                if (resumeText != null && !resumeText.isEmpty() && resumeText.length() >= 10) {
                    String truncated = truncateResume(resumeText);
                    log.info("本地简历解析成功，长度: {}", resumeText.length());
                    return "【简历信息】：\n" + truncated;
                }
            } catch (Exception e) {
                log.warn("本地简历解析失败: {}", e.getMessage());
            }
            log.error("本地简历文件解析失败，无法获取简历内容。path={}", path);
            return "【简历信息】：未上传简历（简历解析失败，请检查文件格式）";
        }

        // 以下处理 OSS / HTTP URL 场景
        boolean isOssUrl = path.contains("aliyuncs.com") || path.contains("oss-");

        if (ossService != null && isOssUrl) {
            try {
                log.info("尝试使用OSS SDK下载简历...");
                byte[] fileData = ossService.downloadByPublicUrl(path);
                if (fileData != null && fileData.length > 0) {
                    log.info("OSS SDK下载成功，文件大小: {} bytes", fileData.length);
                    String resumeText = FileParserUtils.parseBytes(fileData, path);
                    if (resumeText != null && !resumeText.isEmpty() && resumeText.length() >= 10) {
                        String truncated = truncateResume(resumeText);
                        log.info("简历解析成功，长度: {}", resumeText.length());
                        return "【简历信息】：\n" + truncated;
                    } else {
                        log.warn("OSS SDK下载的简历解析结果为空或太短");
                    }
                } else {
                    log.warn("OSS SDK下载返回空数据");
                }
            } catch (Exception e) {
                log.warn("OSS SDK下载简历失败: {}", e.getMessage());
            }
        }

        // 使用 HTTP 下载（适用于公开 OSS URL 或其他可访问的 HTTP 路径）
        try {
            log.info("尝试HTTP方式下载简历: {}", path);
            String resumeText = FileParserUtils.parseFile(path);
            if (resumeText != null && !resumeText.isEmpty() && resumeText.length() >= 10) {
                String truncated = truncateResume(resumeText);
                log.info("HTTP下载简历解析成功，长度: {}", resumeText.length());
                return "【简历信息】：\n" + truncated;
            }
        } catch (Exception e) {
            log.error("HTTP下载简历失败: {}", e.getMessage());
        }

        // 所有方式都失败
        log.error("简历文件解析失败，无法获取简历内容。path={}", path);
        return "【简历信息】：未上传简历（简历解析失败，请检查文件格式）";
    }

    private String truncateResume(String text) {
        if (text.length() > resumeMaxTextLength) {
            return text.substring(0, resumeMaxTextLength) + "\n...[内容已截断]";
        }
        return text;
    }

    @Override
    public String generateMatchAnalysis(String studentProfile, String jobProfile) {
        return generateMatchAnalysis(studentProfile, jobProfile, "");
    }

    @Override
    public String generateMatchAnalysis(String studentProfile, String jobProfile, String knowledgeContext) {
        String ctx = (knowledgeContext != null && !knowledgeContext.isEmpty())
                ? knowledgeContext : "无";
        String prompt = PromptTemplate.MATCH_ANALYSIS_PROMPT
                .replace("{0}", studentProfile)
                .replace("{1}", jobProfile)
                .replace("{2}", ctx);
        return extractJson(callChat(prompt));
    }

    @Override
    public String generateCareerPath(StudentInfo studentInfo, JobBasicInfo targetJob,
                                      String promotionContext, String transferContext) {
        String promoCtx = (promotionContext != null && !promotionContext.isEmpty())
                ? promotionContext : "无";
        String transferCtx = (transferContext != null && !transferContext.isEmpty())
                ? transferContext : "无";
        String prompt = PromptTemplate.CAREER_PATH_PROMPT
                .replace("{0}", JSON.toJSONString(studentInfo))
                .replace("{1}", JSON.toJSONString(targetJob))
                .replace("{2}", promoCtx)
                .replace("{3}", transferCtx);
        return extractJson(callChat(prompt));
    }

    @Override
    public String generate(String prompt) {
        return callChat(prompt);
    }

    @Override
    public String generateCareerPlanningReport(String studentInfoJson, String jobProfileJson) {
        return generateCareerPlanningReport(studentInfoJson, jobProfileJson, "");
    }

    @Override
    public String generateCareerPlanningReport(String studentInfoJson, String jobProfileJson, String knowledgeContext) {
        try {
            JSONObject student = JSON.parseObject(studentInfoJson);

            String studentName = StringUtil.defaultIfBlank(student.getString("name"), "同学");
            String studentMajor = StringUtil.defaultIfBlank(student.getString("major"), "未知专业");
            String studentEducation = StringUtil.defaultIfBlank(student.getString("education"), "本科");
            String graduationYear = StringUtil.defaultIfBlank(student.getString("graduationYear"), "2026");
            String careerIntention = StringUtil.defaultIfBlank(student.getString("careerIntention"), "未明确");
            String personalityTraits = StringUtil.defaultIfBlank(student.getString("personalityTraits"), "未描述");

            String prompt = PromptTemplate.CAREER_PLANNING_REPORT_PROMPT
                    .replace("{0}", studentName)
                    .replace("{1}", studentMajor)
                    .replace("{2}", studentEducation)
                    .replace("{3}", graduationYear)
                    .replace("{4}", careerIntention)
                    .replace("{5}", personalityTraits)
                    .replace("{6}", "（由系统根据上下文信息注入）")
                    .replace("{7}", jobProfileJson);

            if (StringUtil.isNotBlank(knowledgeContext)) {
                prompt = prompt + "\n\n# 上下文信息（系统生成，必须以此为准）\n" + knowledgeContext;
            }

            return extractJson(callChat(prompt));
        } catch (Exception e) {
            log.error("生成职业生涯发展报告失败", e);
            throw new BusinessException("生成报告失败: " + e.getMessage());
        }
    }

    @Override
    public String generateCareerPlanningReport(String studentInfoJson, String jobProfileJson, String knowledgeContext, String targetJobName) {
        try {
            JSONObject student = JSON.parseObject(studentInfoJson);

            String studentName = StringUtil.defaultIfBlank(student.getString("name"), "同学");
            String studentMajor = StringUtil.defaultIfBlank(student.getString("major"), "未知专业");
            String studentEducation = StringUtil.defaultIfBlank(student.getString("education"), "本科");
            String graduationYear = StringUtil.defaultIfBlank(student.getString("graduationYear"), "2026");
            String careerIntention = StringUtil.defaultIfBlank(student.getString("careerIntention"), "未明确");
            String personalityTraits = StringUtil.defaultIfBlank(student.getString("personalityTraits"), "未描述");
            String jobTitle = StringUtil.defaultIfBlank(targetJobName, "目标岗位");

            String prompt = PromptTemplate.CAREER_PLANNING_REPORT_PROMPT
                    .replace("{0}", studentName)
                    .replace("{1}", studentMajor)
                    .replace("{2}", studentEducation)
                    .replace("{3}", graduationYear)
                    .replace("{4}", careerIntention)
                    .replace("{5}", personalityTraits)
                    .replace("{6}", jobTitle)
                    .replace("{7}", jobProfileJson);

            if (StringUtil.isNotBlank(knowledgeContext)) {
                prompt = prompt + "\n\n# 上下文信息（系统生成，必须以此为准）\n" + knowledgeContext;
            }

            return extractJson(callChat(prompt));
        } catch (Exception e) {
            log.error("生成职业生涯发展报告失败", e);
            throw new BusinessException("生成报告失败: " + e.getMessage());
        }
    }

    private String callChat(String prompt) {
        // AI服务重试机制：每次重试都重新构建 request 对象，避免连接复用问题
        BusinessException lastException = null;
        for (int attempt = 1; attempt <= maxRetryAttempts; attempt++) {
            try {
                // 每次重试都重新构建请求体和Request对象
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", CHAT_MODEL);
                requestBody.put("stream", false);
                JSONObject extraBody = new JSONObject();
                extraBody.put("enable_thinking", false);
                extraBody.put("thinking_budget", 0);
                requestBody.put("extra_body", extraBody);

                JSONArray messages = new JSONArray();
                JSONObject userMessage = new JSONObject();
                userMessage.put("role", "user");
                userMessage.put("content", prompt);
                messages.add(userMessage);
                requestBody.put("messages", messages);

                Request request = new Request.Builder()
                        .url(CHAT_API_URL)
                        .addHeader("Authorization", "Bearer " + dashScopeConfig.getApiKey())
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(requestBody.toJSONString(), MEDIA_TYPE_JSON))
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "无响应体";
                        String errorMsg = String.format("百炼API调用失败: HTTP %d - %s", response.code(), errorBody);
                        log.error("调用失败 (尝试 {}/{}): {}", attempt, maxRetryAttempts, errorMsg);
                        // HTTP 5xx 错误可重试，4xx 错误不重试
                        if (response.code() >= 500 && attempt < maxRetryAttempts) {
                            lastException = new BusinessException(errorMsg);
                            sleepBeforeRetry(attempt);
                            continue;
                        }
                        throw new BusinessException(errorMsg);
                    }

                    String responseBody = response.body().string();
                    log.info("百炼API响应成功，内容长度: {} bytes", responseBody.length());
                    return parseResponseContent(responseBody);
                }
            } catch (BusinessException e) {
                throw e;
            } catch (IOException e) {
                log.error("百炼API请求异常 (尝试 {}/{}): {}", attempt, maxRetryAttempts, e.getMessage());
                if (attempt < maxRetryAttempts) {
                    lastException = new BusinessException("百炼服务异常: " + e.getMessage());
                    sleepBeforeRetry(attempt);
                } else {
                    throw new BusinessException("百炼服务异常: " + e.getMessage());
                }
            }
        }
        throw lastException != null ? lastException : new BusinessException("百炼服务调用失败");
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            long sleepTime = retryBackoffMs * (long) Math.pow(2, attempt - 1);
            log.info("等待 {}ms 后重试...", sleepTime);
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String parseResponseContent(String responseBody) {
        try {
            JSONObject responseJson = JSON.parseObject(responseBody);

            // 优先从 choices[0].message.content 获取
            if (responseJson.containsKey("choices")) {
                JSONArray choices = responseJson.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject firstChoice = choices.getJSONObject(0);
                    if (firstChoice.containsKey("message")) {
                        JSONObject message = firstChoice.getJSONObject("message");
                        if (message.containsKey("content")) {
                            String content = message.getString("content");
                            if (content != null && !content.isEmpty()) {
                                return content;
                            }
                        }
                    }
                    if (firstChoice.containsKey("finish_reason")) {
                        String finishReason = firstChoice.getString("finish_reason");
                        if ("length".equals(finishReason)) {
                            log.warn("AI响应被截断（finish_reason=length）");
                        }
                    }
                }
            }

            // 兜底：从 output.choices 获取（新版API格式）
            if (responseJson.containsKey("output")) {
                JSONObject output = responseJson.getJSONObject("output");
                if (output != null && output.containsKey("choices")) {
                    JSONArray choices = output.getJSONArray("choices");
                    if (choices != null && !choices.isEmpty()) {
                        JSONObject firstChoice = choices.getJSONObject(0);
                        if (firstChoice.containsKey("message")) {
                            JSONObject message = firstChoice.getJSONObject("message");
                            if (message.containsKey("content")) {
                                String content = message.getString("content");
                                if (content != null && !content.isEmpty()) {
                                    return content;
                                }
                            }
                        }
                    }
                }
            }

            // 检查是否有错误字段
            if (responseJson.containsKey("error")) {
                JSONObject error = responseJson.getJSONObject("error");
                String errorMsg = error.getString("message");
                log.error("百炼API返回错误: {}", errorMsg);
                throw new BusinessException("AI服务错误: " + errorMsg);
            }

            log.error("百炼API响应格式异常，无法解析内容: {}", truncateForLog(responseBody));
            throw new BusinessException("AI响应格式异常，无法解析内容");

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析百炼响应失败: {}", e.getMessage(), e);
            throw new BusinessException("解析AI响应失败: " + e.getMessage());
        }
    }

    /**
     * 从AI返回内容中提取有效JSON
     * 支持：直接JSON、markdown代码块、嵌套JSON
     */
    private String extractJson(String content) {
        if (content == null || content.trim().isEmpty()) {
            log.error("AI返回内容为空，将返回空JSON");
            return "{}";
        }

        log.info("开始解析AI响应，内容长度: {}", content.length());

        // 去掉思考标签
        content = content.replaceAll("(?s)<\\|reserved_think\\|>[\\s\\S]*?<\\|reserved_think\\|>", "");
        content = content.replaceAll("(?s)<\\|thinking\\|>[\\s\\S]*?<\\|thinking\\|>", "");
        content = content.replaceAll("(?s)<\\|im_start\\|>think[\\s\\S]*?<\\|im_end\\|>", "");
        content = content.replaceAll("(?s)<\\|im_start\\|>result[\\s\\S]*?<\\|im_end\\|>", "");
        content = content.trim();

        log.info("去掉思考标签后内容长度: {}, 前200字符: {}", content.length(), truncateForLog(content));

        // 方式1：直接是JSON
        if (content.startsWith("{") && content.endsWith("}")) {
            if (isValidJsonStructure(content)) {
                log.info("直接识别为有效JSON，长度: {}", content.length());
                return content;
            }
        }

        // 方式2：从markdown代码块中提取
        if (content.contains("```")) {
            Pattern codeBlockPattern = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);
            Matcher matcher = codeBlockPattern.matcher(content);
            while (matcher.find()) {
                String block = matcher.group(1).trim();
                if (block.startsWith("{") && block.endsWith("}")) {
                    if (isValidJsonStructure(block)) {
                        log.info("从markdown代码块中提取到有效JSON，长度: {}", block.length());
                        return block;
                    }
                }
            }
        }

        // 方式3：找第一对花括号
        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            String potentialJson = content.substring(firstBrace, lastBrace + 1);
            if (isValidJsonStructure(potentialJson)) {
                log.info("通过花括号匹配提取到有效JSON，长度: {}", potentialJson.length());
                return potentialJson;
            }
        }

        // 所有方式都失败
        log.error("无法从AI返回内容中提取有效JSON！内容前500字符: {}", truncateForLogForExtract(content));
        return "{}";
    }

    private boolean isValidJsonStructure(String str) {
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

    private String truncateForLog(String content) {
        if (content == null) return "null";
        int maxLength = 200;
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    private String truncateForLogForExtract(String content) {
        if (content == null) return "null";
        int maxLength = 500;
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
