package com.career.planning.service;

import com.career.planning.entity.JobBasicInfo;
import com.career.planning.entity.StudentInfo;

/**
 * AI对话服务统一接口
 * 通过配置 ai.provider 切换不同实现：
 * - dashscope: 阿里云百炼（qwen3.6-plus + text-embedding-v4）
 * - zhipu: 智谱AI（GLM-4-Flash + embedding-3）
 */
public interface AIService {

    /**
     * 进度回调接口，用于在AI生成过程中更新进度
     */
    @FunctionalInterface
    interface ProgressCallback {
        void onProgress(int progress, String stepName);
    }

    String generateJobProfile(JobBasicInfo jobInfo);

    String generateJobProfileWithKnowledge(JobBasicInfo jobInfo, String knowledgeContext);

    String generateStudentProfile(StudentInfo studentInfo);

    String generateMatchAnalysis(String studentProfile, String jobProfile);

    String generateMatchAnalysis(String studentProfile, String jobProfile, String knowledgeContext);

    String generateCareerPath(StudentInfo studentInfo, JobBasicInfo targetJob,
                             String promotionContext, String transferContext);

    String generate(String prompt);

    String generateStudentProfile(StudentInfo studentInfo, ProgressCallback callback);

    String generateCareerPlanningReport(String studentInfoJson, String jobProfileJson);

    String generateCareerPlanningReport(String studentInfoJson, String jobProfileJson, String knowledgeContext);

    String generateCareerPlanningReport(String studentInfoJson, String jobProfileJson, String knowledgeContext, String targetJobName);
}
