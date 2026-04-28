package com.career.planning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.career.planning.entity.JobTransferPath;
import com.career.planning.mapper.JobTransferPathMapper;
import com.career.planning.service.JobTransferPathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobTransferPathServiceImpl extends ServiceImpl<JobTransferPathMapper, JobTransferPath> implements JobTransferPathService {

    @Autowired
    private JobTransferPathMapper jobTransferPathMapper;

    @Override
    public List<JobTransferPath> getTransferPathsBySourceJob(String jobName) {
        return jobTransferPathMapper.selectBySourceJobName(jobName);
    }

    @Override
    public List<JobTransferPath> getTransferPathsByTargetJob(String jobName) {
        return jobTransferPathMapper.selectByTargetJobName(jobName);
    }

    @Override
    public String getTransferPathContext(String jobName) {
        List<JobTransferPath> paths = getTransferPathsBySourceJob(jobName);
        
        if (paths == null || paths.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("## 岗位换岗路径\n\n");
        
        for (JobTransferPath path : paths) {
            context.append("### ").append(path.getSourceJobName())
                   .append(" → ").append(path.getTargetJobName()).append("\n");
            
            if (path.getDifficultyLevel() != null) {
                String stars = "⭐".repeat(path.getDifficultyLevel());
                context.append("**转换难度**：").append(stars).append(" (").append(path.getDifficultyLevel()).append("/5)\n");
            }
            if (path.getTransferDuration() != null) {
                context.append("**转换周期**：").append(path.getTransferDuration()).append("\n");
            }
            if (path.getRequiredSkills() != null) {
                context.append("**所需技能**：").append(path.getRequiredSkills()).append("\n");
            }
            if (path.getAdvantageAnalysis() != null) {
                context.append("**优势分析**：").append(path.getAdvantageAnalysis()).append("\n");
            }
            context.append("\n");
        }
        
        return context.toString();
    }
}
