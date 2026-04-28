package com.career.planning.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.career.planning.entity.StudentAbilityProfile;
import com.career.planning.mapper.StudentAbilityProfileMapper;
import com.career.planning.service.StudentAbilityProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentAbilityProfileServiceImpl extends ServiceImpl<StudentAbilityProfileMapper, StudentAbilityProfile> implements StudentAbilityProfileService {

    private static final String DIM_PROFESSIONAL = "专业技能";
    private static final String DIM_CERTIFICATE = "证书";
    private static final String DIM_INNOVATION = "创新能力";
    private static final String DIM_LEARNING = "学习能力";
    private static final String DIM_PRESSURE = "抗压能力";
    private static final String DIM_COMMUNICATION = "沟通能力";
    private static final String DIM_INTERNSHIP = "实习能力";
    private static final String DIM_OVERALL = "综合";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProfileFromJson(Long studentId, String profileJson) {
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("studentId invalid");
        }
        if (profileJson == null || profileJson.trim().isEmpty()) {
            throw new IllegalArgumentException("profileJson empty");
        }

        JSONObject root = JSON.parseObject(profileJson);

        remove(new QueryWrapper<StudentAbilityProfile>().eq("student_id", studentId));

        List<StudentAbilityProfile> rows = new ArrayList<>();

        // 专业技能
        JSONObject prof = root.getJSONObject("professional_skills");
        if (prof != null) {
            rows.add(row(studentId, DIM_PROFESSIONAL, prof.getBigDecimal("score"), 
                    null, null, prof.toJSONString(), null));
        }

        // 证书
        JSONObject cert = root.getJSONObject("certificates");
        if (cert != null) {
            rows.add(row(studentId, DIM_CERTIFICATE, cert.getBigDecimal("score"),
                    null, null, cert.toJSONString(), null));
        }

        // 软技能（拆成4个维度，便于后续人岗匹配对齐）
        JSONObject soft = root.getJSONObject("soft_skills");
        if (soft != null) {
            addSoft(rows, studentId, DIM_INNOVATION, soft.getJSONObject("innovation_ability"));
            addSoft(rows, studentId, DIM_LEARNING, soft.getJSONObject("learning_ability"));
            addSoft(rows, studentId, DIM_PRESSURE, soft.getJSONObject("pressure_resistance"));
            addSoft(rows, studentId, DIM_COMMUNICATION, soft.getJSONObject("communication_ability"));
        }

        // 实习
        JSONObject intern = root.getJSONObject("internship_experience");
        if (intern != null) {
            rows.add(row(studentId, DIM_INTERNSHIP, intern.getBigDecimal("score"),
                    null, null, intern.toJSONString(), null));
        }

        // 综合（包含完整度、竞争力、优势项等）
        JSONObject overall = root.getJSONObject("overall_assessment");
        if (overall != null) {
            String shortcomings = null;
            String strengths = null;
            if (overall.getJSONArray("shortcomings") != null) {
                shortcomings = overall.getJSONArray("shortcomings").toJSONString();
            }
            if (overall.getJSONArray("strengths") != null) {
                strengths = overall.getJSONArray("strengths").toJSONString();
            }
            // 计算完整度和竞争力评分（从各维度综合评估）
            BigDecimal completenessScore = calculateCompletenessScore(root);
            BigDecimal competitivenessScore = overall.getBigDecimal("total_score");
            rows.add(row(studentId, DIM_OVERALL, overall.getBigDecimal("total_score"),
                    completenessScore, competitivenessScore, overall.toJSONString(), shortcomings, strengths));
        }

        if (!rows.isEmpty()) {
            saveBatch(rows);
        }
    }

    /**
     * 计算学生画像的完整度评分
     * 基于已填写的维度数量和质量
     */
    private BigDecimal calculateCompletenessScore(JSONObject root) {
        int totalFields = 0;
        int filledFields = 0;
        
        // 检查专业技能
        totalFields++;
        if (root.getJSONObject("professional_skills") != null 
                && root.getJSONObject("professional_skills").getJSONArray("skills") != null
                && !root.getJSONObject("professional_skills").getJSONArray("skills").isEmpty()) {
            filledFields++;
        }
        
        // 检查证书
        totalFields++;
        if (root.getJSONObject("certificates") != null 
                && root.getJSONObject("certificates").getJSONArray("owned") != null
                && !root.getJSONObject("certificates").getJSONArray("owned").isEmpty()) {
            filledFields++;
        }
        
        // 检查软技能（4项）
        JSONObject soft = root.getJSONObject("soft_skills");
        if (soft != null) {
            String[] skills = {"innovation_ability", "learning_ability", "pressure_resistance", "communication_ability"};
            for (String skill : skills) {
                totalFields++;
                if (soft.getJSONObject(skill) != null) {
                    filledFields++;
                }
            }
        } else {
            totalFields += 4;
        }
        
        // 检查实习经历
        totalFields++;
        if (root.getJSONObject("internship_experience") != null
                && root.getJSONObject("internship_experience").getJSONArray("experiences") != null
                && !root.getJSONObject("internship_experience").getJSONArray("experiences").isEmpty()) {
            filledFields++;
        }
        
        // 计算百分比
        if (totalFields == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(filledFields * 100.0 / totalFields).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getLatestProfileJson(Long studentId) {
        if (studentId == null || studentId <= 0) {
            return null;
        }
        List<StudentAbilityProfile> list = list(new QueryWrapper<StudentAbilityProfile>()
                .eq("student_id", studentId)
                .orderByDesc("created_at"));
        if (list == null || list.isEmpty()) {
            return null;
        }

        JSONObject out = new JSONObject();
        for (StudentAbilityProfile p : list) {
            String dim = p.getDimension();
            if (dim == null) {
                continue;
            }
            String details = p.getDetails();
            if (details == null || details.isEmpty()) {
                continue;
            }
            try {
                JSONObject obj = JSON.parseObject(details);
                switch (dim) {
                    case DIM_PROFESSIONAL -> out.put("professional_skills", obj);
                    case DIM_CERTIFICATE -> out.put("certificates", obj);
                    case DIM_INNOVATION -> ensureSoft(out).put("innovation_ability", obj);
                    case DIM_LEARNING -> ensureSoft(out).put("learning_ability", obj);
                    case DIM_PRESSURE -> ensureSoft(out).put("pressure_resistance", obj);
                    case DIM_COMMUNICATION -> ensureSoft(out).put("communication_ability", obj);
                    case DIM_INTERNSHIP -> out.put("internship_experience", obj);
                    case DIM_OVERALL -> {
                        out.put("overall_assessment", obj);
                        // 额外输出完整度和竞争力评分
                        if (p.getCompletenessScore() != null) {
                            obj.put("completeness_score", p.getCompletenessScore());
                        }
                        if (p.getCompetitivenessScore() != null) {
                            obj.put("competitiveness_score", p.getCompetitivenessScore());
                        }
                        if (p.getStrengths() != null) {
                            try {
                                obj.put("strengths", JSON.parseArray(p.getStrengths()));
                            } catch (Exception e) {
                                obj.put("strengths", p.getStrengths());
                            }
                        }
                    }
                    default -> {
                        // ignore unknown dimensions
                    }
                }
            } catch (Exception ignored) {
                // skip bad rows
            }
        }

        // basic_info 目前不入库：保持为空即可（报告生成主要依赖各维度与综合）
        return out.isEmpty() ? null : out.toJSONString();
    }

    private static JSONObject ensureSoft(JSONObject root) {
        JSONObject soft = root.getJSONObject("soft_skills");
        if (soft == null) {
            soft = new JSONObject();
            root.put("soft_skills", soft);
        }
        return soft;
    }

    private static void addSoft(List<StudentAbilityProfile> rows, Long studentId, String dimension, JSONObject node) {
        if (node == null) {
            return;
        }
        rows.add(row(studentId, dimension, node.getBigDecimal("score"), null, null, node.toJSONString(), null));
    }

    private static StudentAbilityProfile row(Long studentId, String dimension, BigDecimal score, 
            BigDecimal completenessScore, BigDecimal competitivenessScore,
            String details, String shortcomings, String... strengths) {
        StudentAbilityProfile p = new StudentAbilityProfile();
        p.setStudentId(studentId);
        p.setDimension(dimension);
        p.setScore(normalizeScore(score));
        p.setCompletenessScore(completenessScore != null ? completenessScore.setScale(2, RoundingMode.HALF_UP) : null);
        p.setCompetitivenessScore(competitivenessScore != null ? competitivenessScore.setScale(2, RoundingMode.HALF_UP) : null);
        p.setDetails(details);
        p.setShortcomings(shortcomings);
        p.setStrengths(strengths != null && strengths.length > 0 ? strengths[0] : null);
        p.setCreatedAt(LocalDateTime.now());
        return p;
    }

    private static BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }
}
