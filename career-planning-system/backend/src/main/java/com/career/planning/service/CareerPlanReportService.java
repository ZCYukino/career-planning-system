package com.career.planning.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.career.planning.entity.CareerPlanReport;

public interface CareerPlanReportService extends IService<CareerPlanReport> {
    
    /**
     * 异步创建职业规划报告任务
     * 立即返回任务ID，前端通过轮询 /report/status/{taskId} 获取进度
     * @param studentId 学生ID
     * @param targetJobName 目标岗位名称
     * @return 创建的报告记录（初始状态为 PROCESSING）
     */
    CareerPlanReport createReportTask(Long studentId, String targetJobName);

    /**
     * 同步生成职业规划报告（保留兼容）
     * @param studentId 学生ID
     * @param targetJobName 目标岗位名称
     * @return 生成的报告
     */
    CareerPlanReport generateReport(Long studentId, String targetJobName);

    /**
     * 获取最新的职业规划报告
     * @param studentId 学生ID
     * @return 最新报告，如果不存在返回null
     */
    CareerPlanReport getLatestReport(Long studentId);

    /**
     * 更新报告内容（用于前端编辑后持久化）
     */
    CareerPlanReport updateReportContent(Long reportId, Long studentId, String reportContent);
}
