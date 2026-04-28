package com.career.planning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.career.planning.entity.JobPromotionRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 岗位晋升关系Mapper
 * 支持基于ID和基于名称的两种查询方式
 */
@Mapper
public interface JobPromotionRelationMapper extends BaseMapper<JobPromotionRelation> {

    /**
     * 通过岗位名称查询晋升路径
     */
    @Select("SELECT * FROM job_promotion_relation " +
            "WHERE current_job_name = #{jobName} " +
            "ORDER BY id")
    List<JobPromotionRelation> selectByJobName(@Param("jobName") String jobName);

    /**
     * 获取所有晋升路径（带名称）
     */
    @Select("SELECT jpr.*, " +
            "j1.job_name as current_job_name, " +
            "j2.job_name as next_job_name " +
            "FROM job_promotion_relation jpr " +
            "LEFT JOIN job_basic_info j1 ON jpr.current_job_id = j1.id " +
            "LEFT JOIN job_basic_info j2 ON jpr.next_job_id = j2.id " +
            "ORDER BY jpr.current_job_id, jpr.id")
    List<Map<String, Object>> selectAllWithJobName();

    /**
     * 通过岗位名称获取晋升路径
     */
    @Select("SELECT jpr.*, " +
            "j1.job_name as current_job_name, " +
            "j2.job_name as next_job_name " +
            "FROM job_promotion_relation jpr " +
            "LEFT JOIN job_basic_info j1 ON jpr.current_job_id = j1.id " +
            "LEFT JOIN job_basic_info j2 ON jpr.next_job_id = j2.id " +
            "WHERE j1.job_name = #{jobName} OR jpr.current_job_name = #{jobName} " +
            "ORDER BY jpr.id")
    List<Map<String, Object>> selectByJobNameWithJoin(@Param("jobName") String jobName);

    /**
     * 获取所有晋升路径（仅使用名称）
     */
    @Select("SELECT * FROM job_promotion_relation " +
            "WHERE current_job_name IS NOT NULL " +
            "ORDER BY current_job_name, id")
    List<JobPromotionRelation> selectAllByName();
}
