package com.career.planning.common;

/**
 * 画像相关常量
 * 集中管理兜底画像JSON等常量，避免在多处重复定义
 */
public final class ProfileConstants {

    private ProfileConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /**
     * 兜底学生画像JSON
     * 当AI服务不可用或生成失败时使用此默认画像
     * 使用位置：CareerPlanReportServiceImpl / StudentInfoController
     */
    public static final String FALLBACK_STUDENT_PROFILE = """
            {
                "professional_skills": {
                    "skills": [],
                    "proficiency": {},
                    "score": 50,
                    "analysis": "专业技能信息不足，请补充专业技能、项目经历或相关证书来完善评估。"
                },
                "certificates": {
                    "owned": [],
                    "score": 30,
                    "analysis": "未检测到相关证书信息。建议补充专业类证书以提升竞争力。"
                },
                "soft_skills": {
                    "innovation_ability": {"score": 60, "evidence": "暂无相关经历证明，建议通过项目实践展示创新能力", "improvement": "建议参与课程项目或竞赛活动"},
                    "learning_ability": {"score": 65, "evidence": "暂无明确的学习成果证明，建议补充学习经历", "improvement": "建议参与线上课程或技术博客输出"},
                    "pressure_resistance": {"score": 60, "evidence": "暂无高压环境下的经历证明", "improvement": "建议通过竞赛、限时项目等场景锻炼抗压能力"},
                    "communication_ability": {"score": 65, "evidence": "暂无团队协作经历的明确证明", "improvement": "建议参与团队项目或社团活动提升沟通能力"}
                },
                "internship_experience": {
                    "experiences": [],
                    "score": 30,
                    "analysis": "暂无实习经历记录，建议补充相关经历以提升简历竞争力。"
                },
                "overall_assessment": {
                    "total_score": 55,
                    "competitiveness_level": "待提升",
                    "strengths": ["信息完整后可获得更准确的能力评估"],
                    "shortcomings": ["专业信息不足", "建议完善简历内容"],
                    "improvement_priority": ["补充专业技能和项目经历", "完善简历信息后重新生成画像"]
                }
            }
            """;
}
