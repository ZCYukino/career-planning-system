package com.career.planning.controller;

import com.career.planning.common.CommonResult;
import com.career.planning.entity.JobPromotionRelation;
import com.career.planning.entity.JobTransferPath;
import com.career.planning.service.JobPromotionRelationService;
import com.career.planning.service.JobTransferPathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/career-path")
public class CareerPathController {

    @Autowired
    private JobPromotionRelationService jobPromotionRelationService;

    @Autowired
    private JobTransferPathService jobTransferPathService;

    @GetMapping("/promotion/{jobName}")
    public CommonResult<List<Map<String, Object>>> getPromotionPath(@PathVariable(value = "jobName") String jobName) {
        if (!StringUtils.hasText(jobName)) {
            return CommonResult.failed("岗位名称不能为空");
        }
        return CommonResult.success(jobPromotionRelationService.getPromotionPathByJobName(jobName.trim()));
    }

    @GetMapping("/promotion/all")
    public CommonResult<List<Map<String, Object>>> getAllPromotionPaths() {
        return CommonResult.success(jobPromotionRelationService.getAllPromotionPaths());
    }

    @GetMapping("/transfer/{jobName}")
    public CommonResult<List<JobTransferPath>> getTransferPaths(@PathVariable(value = "jobName") String jobName) {
        if (!StringUtils.hasText(jobName)) {
            return CommonResult.failed("岗位名称不能为空");
        }
        return CommonResult.success(jobTransferPathService.getTransferPathsBySourceJob(jobName.trim()));
    }

    @GetMapping("/paths")
    public CommonResult<Map<String, Object>> getCareerPaths(@RequestParam(value = "jobName") String jobName) {
        if (!StringUtils.hasText(jobName)) {
            return CommonResult.failed("岗位名称不能为空");
        }
        String trimmedJobName = jobName.trim();
        Map<String, Object> result = new HashMap<>();
        
        result.put("promotionPaths", jobPromotionRelationService.getPromotionPathByJobName(trimmedJobName));
        result.put("transferPaths", jobTransferPathService.getTransferPathsBySourceJob(trimmedJobName));
        result.put("promotionContext", jobPromotionRelationService.getPromotionPathContext(trimmedJobName));
        result.put("transferContext", jobTransferPathService.getTransferPathContext(trimmedJobName));
        
        return CommonResult.success(result);
    }

    @GetMapping("/context/{jobName}")
    public CommonResult<String> getCareerPathContext(@PathVariable(value = "jobName") String jobName) {
        if (!StringUtils.hasText(jobName)) {
            return CommonResult.failed("岗位名称不能为空");
        }
        String trimmedJobName = jobName.trim();
        StringBuilder context = new StringBuilder();
        
        String promotionContext = jobPromotionRelationService.getPromotionPathContext(trimmedJobName);
        String transferContext = jobTransferPathService.getTransferPathContext(trimmedJobName);
        
        if (promotionContext != null && !promotionContext.isEmpty()) {
            context.append(promotionContext).append("\n");
        }
        
        if (transferContext != null && !transferContext.isEmpty()) {
            context.append(transferContext);
        }
        
        return CommonResult.success(context.toString());
    }

    @PostMapping("/promotion")
    public CommonResult<String> addPromotionPath(@RequestBody JobPromotionRelation relation) {
        if (relation == null) {
            return CommonResult.failed("晋升路径数据不能为空");
        }
        jobPromotionRelationService.save(relation);
        return CommonResult.success("添加成功");
    }

    @PostMapping("/transfer")
    public CommonResult<String> addTransferPath(@RequestBody JobTransferPath path) {
        if (path == null) {
            return CommonResult.failed("换岗路径数据不能为空");
        }
        jobTransferPathService.save(path);
        return CommonResult.success("添加成功");
    }
}
