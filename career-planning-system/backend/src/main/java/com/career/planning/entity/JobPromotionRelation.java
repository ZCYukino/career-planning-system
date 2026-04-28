package com.career.planning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job_promotion_relation")
public class JobPromotionRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 当前岗位ID（可为空，使用名称关联）
     */
    private Long currentJobId;

    /**
     * 当前岗位名称（用于直接关联）
     */
    private String currentJobName;

    /**
     * 晋升目标岗位ID（可为空，使用名称关联）
     */
    private Long nextJobId;

    /**
     * 晋升目标岗位名称（用于直接关联）
     */
    private String nextJobName;

    /**
     * 晋升路径描述
     */
    private String promotionPathDesc;

    /**
     * 所需技能补充
     */
    private String requiredSkills;

    /**
     * 经验年限要求
     */
    private String experienceYears;

    /**
     * 薪资涨幅范围
     */
    private String salaryIncreaseRange;

    private LocalDateTime createdAt;
}
