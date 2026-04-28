package com.career.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("job_transfer_path")
public class JobTransferPath {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sourceJobId;

    private String sourceJobName;

    private String targetJobName;

    private Integer difficultyLevel;

    private String requiredSkills;

    private String transferDuration;

    private String advantageAnalysis;

    private Integer pathIndex;

    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSourceJobId() { return sourceJobId; }
    public void setSourceJobId(Long sourceJobId) { this.sourceJobId = sourceJobId; }

    public String getSourceJobName() { return sourceJobName; }
    public void setSourceJobName(String sourceJobName) { this.sourceJobName = sourceJobName; }

    public String getTargetJobName() { return targetJobName; }
    public void setTargetJobName(String targetJobName) { this.targetJobName = targetJobName; }

    public Integer getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(Integer difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }

    public String getTransferDuration() { return transferDuration; }
    public void setTransferDuration(String transferDuration) { this.transferDuration = transferDuration; }

    public String getAdvantageAnalysis() { return advantageAnalysis; }
    public void setAdvantageAnalysis(String advantageAnalysis) { this.advantageAnalysis = advantageAnalysis; }

    public Integer getPathIndex() { return pathIndex; }
    public void setPathIndex(Integer pathIndex) { this.pathIndex = pathIndex; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
