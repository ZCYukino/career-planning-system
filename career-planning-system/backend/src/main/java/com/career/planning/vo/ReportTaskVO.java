package com.career.planning.vo;

import com.career.planning.entity.CareerPlanReport;
import java.math.BigDecimal;

/**
 * 报告生成任务状态 VO
 * 用于前端轮询获取任务进度
 */
public class ReportTaskVO {
    /** 报告ID */
    private Long reportId;
    /** 任务状态：PROCESSING-生成中, FINAL-已完成, FAILED-失败 */
    private String status;
    /** 生成进度 0-100 */
    private Integer progress;
    /** 当前阶段 1-4 */
    private Integer currentStep;
    /** 当前阶段名称 */
    private String stepName;
    /** 错误信息 */
    private String errorMessage;
    /** 完整的报告内容（仅完成后有值） */
    private String reportContent;
    /** 匹配分数（完成后有值） */
    private BigDecimal matchScore;
    /** 目标岗位ID */
    private Long targetJobId;
    /** 阶段1：学生画像（完成后有值） */
    private String studentProfileJson;
    /** 阶段2：岗位画像（完成后有值） */
    private String jobProfileJson;
    /** 阶段3：人岗匹配分析（完成后有值） */
    private String matchAnalysisJson;

    // 阶段定义常量
    public static final String STEP_1 = "生成学生画像";
    public static final String STEP_2 = "生成岗位画像";
    public static final String STEP_3 = "分析人岗匹配";
    public static final String STEP_4 = "生成完整报告";

    // 进度映射
    public static final int PROGRESS_START = 5;
    public static final int PROGRESS_STEP1_DONE = 25;
    public static final int PROGRESS_STEP2_DONE = 50;
    public static final int PROGRESS_STEP3_DONE = 75;
    public static final int PROGRESS_COMPLETE = 100;

    public static ReportTaskVO fromReport(CareerPlanReport report) {
        if (report == null) return null;
        ReportTaskVO vo = new ReportTaskVO();
        vo.setReportId(report.getId());
        vo.setStatus(report.getStatus());
        vo.setProgress(report.getProgress());
        vo.setCurrentStep(report.getCurrentStep());
        vo.setStepName(report.getStepName());
        vo.setErrorMessage(report.getErrorMessage());
        vo.setReportContent(report.getReportContent());
        vo.setMatchScore(report.getMatchScore());
        vo.setTargetJobId(report.getTargetJobId());
        vo.setStudentProfileJson(report.getStudentProfileJson());
        vo.setJobProfileJson(report.getJobProfileJson());
        vo.setMatchAnalysisJson(report.getMatchAnalysisJson());
        return vo;
    }

    // Getters and Setters
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public Integer getCurrentStep() { return currentStep; }
    public void setCurrentStep(Integer currentStep) { this.currentStep = currentStep; }

    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getReportContent() { return reportContent; }
    public void setReportContent(String reportContent) { this.reportContent = reportContent; }

    public BigDecimal getMatchScore() { return matchScore; }
    public void setMatchScore(BigDecimal matchScore) { this.matchScore = matchScore; }

    public Long getTargetJobId() { return targetJobId; }
    public void setTargetJobId(Long targetJobId) { this.targetJobId = targetJobId; }

    public String getStudentProfileJson() { return studentProfileJson; }
    public void setStudentProfileJson(String studentProfileJson) { this.studentProfileJson = studentProfileJson; }

    public String getJobProfileJson() { return jobProfileJson; }
    public void setJobProfileJson(String jobProfileJson) { this.jobProfileJson = jobProfileJson; }

    public String getMatchAnalysisJson() { return matchAnalysisJson; }
    public void setMatchAnalysisJson(String matchAnalysisJson) { this.matchAnalysisJson = matchAnalysisJson; }
}
