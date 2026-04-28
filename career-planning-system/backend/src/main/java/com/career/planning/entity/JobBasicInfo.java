package com.career.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("job_basic_info")
public class JobBasicInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobCode;
    private String jobName;
    private Integer jobRank;
    private Integer jobCount;
    private BigDecimal popularityScore;
    private String industry;
    private String salaryRange;
    private String workLocation;
    private String companyName;
    private String companySize;
    private String companyNature;
    private String jobDescription;
    private String companyIntro;
    private Boolean isCoreJob;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getJobCode() { return jobCode; }
    public void setJobCode(String jobCode) { this.jobCode = jobCode; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public Integer getJobRank() { return jobRank; }
    public void setJobRank(Integer jobRank) { this.jobRank = jobRank; }

    public Integer getJobCount() { return jobCount; }
    public void setJobCount(Integer jobCount) { this.jobCount = jobCount; }

    public BigDecimal getPopularityScore() { return popularityScore; }
    public void setPopularityScore(BigDecimal popularityScore) { this.popularityScore = popularityScore; }

    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }

    public String getWorkLocation() { return workLocation; }
    public void setWorkLocation(String workLocation) { this.workLocation = workLocation; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }

    public String getCompanySize() { return companySize; }
    public void setCompanySize(String companySize) { this.companySize = companySize; }

    public String getCompanyNature() { return companyNature; }
    public void setCompanyNature(String companyNature) { this.companyNature = companyNature; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public String getCompanyIntro() { return companyIntro; }
    public void setCompanyIntro(String companyIntro) { this.companyIntro = companyIntro; }

    public Boolean getIsCoreJob() { return isCoreJob; }
    public void setIsCoreJob(Boolean isCoreJob) { this.isCoreJob = isCoreJob; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
