package com.career.planning.controller;

import com.career.planning.common.CommonResult;
import com.career.planning.entity.CareerPlanReport;
import com.career.planning.service.CareerPlanReportService;
import com.career.planning.util.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class CareerPlanReportController {

    @Autowired
    private CareerPlanReportService careerPlanReportService;

    /**
     * 创建报告生成任务（异步，返回后立即开始后台生成）
     */
    @PostMapping("/create")
    public CommonResult<Map<String, Object>> createReportTask(@RequestParam Long studentId, @RequestParam String targetJobName) {
        if (studentId == null || studentId <= 0) {
            return CommonResult.failed("学生ID无效");
        }
        if (!StringUtils.hasText(targetJobName)) {
            return CommonResult.failed("目标岗位名称不能为空");
        }
        try {
            CareerPlanReport report = careerPlanReportService.createReportTask(studentId, targetJobName.trim());
            Map<String, Object> result = Map.of(
                    "reportId", report.getId(),
                    "status", report.getStatus(),
                    "progress", report.getProgress() != null ? report.getProgress() : 0,
                    "stepName", report.getStepName() != null ? report.getStepName() : "正在初始化...",
                    "currentStep", report.getCurrentStep() != null ? report.getCurrentStep() : 1
            );
            return CommonResult.success(result);
        } catch (Exception e) {
            return CommonResult.failed("任务创建失败: " + e.getMessage());
        }
    }

    /**
     * 轮询报告生成状态
     */
    @GetMapping("/status/{reportId}")
    public CommonResult<Map<String, Object>> getReportStatus(@PathVariable Long reportId) {
        if (reportId == null || reportId <= 0) {
            return CommonResult.failed("报告ID无效");
        }
        try {
            CareerPlanReport report = careerPlanReportService.getById(reportId);
            if (report == null) {
                return CommonResult.failed("报告不存在");
            }

            // 优先检查 FINAL 状态（正常完成）
            if ("FINAL".equals(report.getStatus()) || "COMPLETED".equals(report.getStatus())) {
                return CommonResult.success(Map.of(
                        "reportId", report.getId(),
                        "status", "COMPLETED",
                        "progress", report.getProgress() != null ? report.getProgress() : 100,
                        "stepName", report.getStepName() != null ? report.getStepName() : "报告生成完成",
                        "report", report
                ));
            }
            
            // 检查失败状态
            if ("FAILED".equals(report.getStatus())) {
                return CommonResult.success(Map.of(
                        "reportId", report.getId(),
                        "status", "FAILED",
                        "progress", 0,
                        "stepName", "生成失败",
                        "errorMessage", report.getErrorMessage() != null ? report.getErrorMessage() : "未知错误"
                ));
            }
            
            // 处理中状态（包括 PROCESSING、PENDING 等）
            Map<String, Object> status = Map.of(
                    "reportId", report.getId(),
                    "status", report.getStatus() != null ? report.getStatus() : "PROCESSING",
                    "progress", report.getProgress() != null ? report.getProgress() : 0,
                    "stepName", report.getStepName() != null ? report.getStepName() : "",
                    "currentStep", report.getCurrentStep() != null ? report.getCurrentStep() : 0,
                    "errorMessage", report.getErrorMessage() != null ? report.getErrorMessage() : ""
            );

            // 如果进度>=70%，说明报告内容已经生成，可以返回完整报告
            if (report.getProgress() != null && report.getProgress() >= 70 
                    && StringUtil.isNotBlank(report.getReportContent())) {
                status = Map.of(
                        "reportId", report.getId(),
                        "status", "PROCESSING",
                        "progress", report.getProgress(),
                        "stepName", report.getStepName() != null ? report.getStepName() : "报告生成中...",
                        "currentStep", report.getCurrentStep() != null ? report.getCurrentStep() : 0,
                        "errorMessage", "",
                        "report", report  // 返回完整报告
                );
            }
            
            return CommonResult.success(status);
        } catch (Exception e) {
            return CommonResult.failed("获取状态失败: " + e.getMessage());
        }
    }

    @PostMapping("/generate")
    public CommonResult<CareerPlanReport> generateReport(@RequestParam Long studentId, @RequestParam String targetJobName) {
        if (studentId == null || studentId <= 0) {
            return CommonResult.failed("学生ID无效");
        }
        if (!StringUtils.hasText(targetJobName)) {
            return CommonResult.failed("目标岗位名称不能为空");
        }
        try {
            return CommonResult.success(careerPlanReportService.generateReport(studentId, targetJobName.trim()));
        } catch (Exception e) {
            return CommonResult.failed("报告生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/latest")
    public CommonResult<CareerPlanReport> getLatestReport(@RequestParam Long studentId) {
        if (studentId == null || studentId <= 0) {
            return CommonResult.failed("学生ID无效");
        }
        CareerPlanReport report = careerPlanReportService.getLatestReport(studentId);
        return CommonResult.success(report);
    }

    @PutMapping("/update")
    public CommonResult<CareerPlanReport> updateReport(@RequestBody Map<String, Object> body) {
        try {
            Object reportIdObj = body.get("reportId");
            Object studentIdObj = body.get("studentId");
            Object contentObj = body.get("reportContent");

            if (reportIdObj == null || studentIdObj == null || contentObj == null) {
                return CommonResult.failed("参数不完整：需要 reportId、studentId、reportContent");
            }

            Long reportId = Long.valueOf(reportIdObj.toString());
            Long studentId = Long.valueOf(studentIdObj.toString());
            String reportContent = contentObj.toString();

            return CommonResult.success(careerPlanReportService.updateReportContent(reportId, studentId, reportContent));
        } catch (Exception e) {
            return CommonResult.failed("报告更新失败: " + e.getMessage());
        }
    }
}
