package com.career.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生能力画像实体
 * 对应数据库表 student_ability_profile
 */
@TableName("student_ability_profile")
public class StudentAbilityProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String dimension;
    private BigDecimal score;
    private BigDecimal completenessScore;
    private BigDecimal competitivenessScore;
    private String details;
    private String shortcomings;
    private String strengths;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    
    public BigDecimal getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(BigDecimal completenessScore) { this.completenessScore = completenessScore; }
    
    public BigDecimal getCompetitivenessScore() { return competitivenessScore; }
    public void setCompetitivenessScore(BigDecimal competitivenessScore) { this.competitivenessScore = competitivenessScore; }
    
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    
    public String getShortcomings() { return shortcomings; }
    public void setShortcomings(String shortcomings) { this.shortcomings = shortcomings; }
    
    public String getStrengths() { return strengths; }
    public void setStrengths(String strengths) { this.strengths = strengths; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
