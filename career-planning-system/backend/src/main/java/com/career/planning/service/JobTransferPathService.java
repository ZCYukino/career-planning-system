package com.career.planning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.career.planning.entity.JobTransferPath;

import java.util.List;

public interface JobTransferPathService extends IService<JobTransferPath> {

    List<JobTransferPath> getTransferPathsBySourceJob(String jobName);

    List<JobTransferPath> getTransferPathsByTargetJob(String jobName);

    String getTransferPathContext(String jobName);
}
