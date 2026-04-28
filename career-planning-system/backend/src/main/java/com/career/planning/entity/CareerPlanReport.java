package com.career.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("career_plan_report")
public class CareerPlanReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long targetJobId;
    /** 目标岗位名称（冗余存储，方便前端直接展示） */
    private String targetJobName;
    private BigDecimal matchScore;
    private String reportContent;
    private String actionPlan;
    /** 报告状态：PROCESSING-生成中, FINAL-已完成, FAILED-失败 */
    private String status;
    /** 生成进度 0-100 */
    private Integer progress;
    /** 当前阶段 1-4 */
    private Integer currentStep;
    /** 当前阶段名称 */
    private String stepName;
    /** 阶段1结果：学生画像JSON */
    private String studentProfileJson;
    /** 阶段2结果：岗位画像JSON */
    private String jobProfileJson;
    /** 阶段3结果：人岗匹配分析JSON */
    private String matchAnalysisJson;
    /** 错误信息 */
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    
    public Long getTargetJobId() { return targetJobId; }
    public void setTargetJobId(Long targetJobId) { this.targetJobId = targetJobId; }

    public String getTargetJobName() { return targetJobName; }
    public void setTargetJobName(String targetJobName) { this.targetJobName = targetJobName; }

    public BigDecimal getMatchScore() { return matchScore; }
    public void setMatchScore(BigDecimal matchScore) { this.matchScore = matchScore; }
    
    public String getReportContent() { return reportContent; }
    public void setReportContent(String reportContent) { this.reportContent = reportContent; }
    
    public String getActionPlan() { return actionPlan; }
    public void setActionPlan(String actionPlan) { this.actionPlan = actionPlan; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    
    public Integer getCurrentStep() { return currentStep; }
    public void setCurrentStep(Integer currentStep) { this.currentStep = currentStep; }
    
    public String getStepName() { return stepName; }
    public void setStepName(String stepName) { this.stepName = stepName; }
    
    public String getStudentProfileJson() { return studentProfileJson; }
    public void setStudentProfileJson(String studentProfileJson) { this.studentProfileJson = studentProfileJson; }
    
    public String getJobProfileJson() { return jobProfileJson; }
    public void setJobProfileJson(String jobProfileJson) { this.jobProfileJson = jobProfileJson; }
    
    public String getMatchAnalysisJson() { return matchAnalysisJson; }
    public void setMatchAnalysisJson(String matchAnalysisJson) { this.matchAnalysisJson = matchAnalysisJson; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
