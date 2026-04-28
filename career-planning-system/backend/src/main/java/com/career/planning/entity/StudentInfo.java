package com.career.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("student_info")
public class StudentInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String name;
    private String gender;
    private String email;
    private Integer age;
    private String major;
    private String graduationSchool;
    private String education;
    private Integer graduationYear;
    private String careerIntention;
    private String personalityTraits;
    private String workExperienceYears;
    private String resumeFilePath;
    @TableField("create_time")
    private LocalDateTime createdAt;
    @TableField("update_time")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    
    public String getGraduationSchool() { return graduationSchool; }
    public void setGraduationSchool(String graduationSchool) { this.graduationSchool = graduationSchool; }
    
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    
    public Integer getGraduationYear() { return graduationYear; }
    public void setGraduationYear(Integer graduationYear) { this.graduationYear = graduationYear; }
    
    public String getCareerIntention() { return careerIntention; }
    public void setCareerIntention(String careerIntention) { this.careerIntention = careerIntention; }
    
    public String getPersonalityTraits() { return personalityTraits; }
    public void setPersonalityTraits(String personalityTraits) { this.personalityTraits = personalityTraits; }
    
    public String getWorkExperienceYears() { return workExperienceYears; }
    public void setWorkExperienceYears(String workExperienceYears) { this.workExperienceYears = workExperienceYears; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getResumeFilePath() { return resumeFilePath; }
    public void setResumeFilePath(String resumeFilePath) { this.resumeFilePath = resumeFilePath; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
