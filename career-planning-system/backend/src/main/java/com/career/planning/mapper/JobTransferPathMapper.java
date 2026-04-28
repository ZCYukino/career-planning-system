package com.career.planning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.career.planning.entity.JobTransferPath;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface JobTransferPathMapper extends BaseMapper<JobTransferPath> {

    @Select("SELECT * FROM job_transfer_path WHERE source_job_name = #{jobName}")
    List<JobTransferPath> selectBySourceJobName(@Param("jobName") String jobName);

    @Select("SELECT * FROM job_transfer_path WHERE target_job_name = #{jobName}")
    List<JobTransferPath> selectByTargetJobName(@Param("jobName") String jobName);
}
