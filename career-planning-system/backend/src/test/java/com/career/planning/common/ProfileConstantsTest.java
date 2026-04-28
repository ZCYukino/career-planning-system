package com.career.planning.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProfileConstants 常量类测试
 * 验证兜底画像JSON结构完整且可解析
 */
class ProfileConstantsTest {

    @Test
    @DisplayName("FALLBACK_STUDENT_PROFILE 应该是非空字符串")
    void fallbackProfileShouldNotBeBlank() {
        assertNotNull(ProfileConstants.FALLBACK_STUDENT_PROFILE);
        assertFalse(ProfileConstants.FALLBACK_STUDENT_PROFILE.trim().isEmpty());
    }

    @Test
    @DisplayName("FALLBACK_STUDENT_PROFILE 应该是合法JSON")
    void fallbackProfileShouldBeValidJson() {
        assertDoesNotThrow(() -> JSON.parseObject(ProfileConstants.FALLBACK_STUDENT_PROFILE));
    }

    @Test
    @DisplayName("FALLBACK_STUDENT_PROFILE 应包含所有必要维度")
    void fallbackProfileShouldContainAllDimensions() {
        JSONObject json = JSON.parseObject(ProfileConstants.FALLBACK_STUDENT_PROFILE);

        assertTrue(json.containsKey("professional_skills"), "缺少 professional_skills 维度");
        assertTrue(json.containsKey("certificates"), "缺少 certificates 维度");
        assertTrue(json.containsKey("soft_skills"), "缺少 soft_skills 维度");
        assertTrue(json.containsKey("internship_experience"), "缺少 internship_experience 维度");
        assertTrue(json.containsKey("overall_assessment"), "缺少 overall_assessment 维度");
    }

    @Test
    @DisplayName("FALLBACK_STUDENT_PROFILE 软技能应包含4个子维度")
    void fallbackProfileShouldContainFourSoftSkills() {
        JSONObject json = JSON.parseObject(ProfileConstants.FALLBACK_STUDENT_PROFILE);
        JSONObject soft = json.getJSONObject("soft_skills");

        assertNotNull(soft);
        assertTrue(soft.containsKey("innovation_ability"), "缺少 innovation_ability");
        assertTrue(soft.containsKey("learning_ability"), "缺少 learning_ability");
        assertTrue(soft.containsKey("pressure_resistance"), "缺少 pressure_resistance");
        assertTrue(soft.containsKey("communication_ability"), "缺少 communication_ability");
    }

    @Test
    @DisplayName("FALLBACK_STUDENT_PROFILE 各维度应包含 score 字段")
    void fallbackProfileShouldHaveScoresInAllDimensions() {
        JSONObject json = JSON.parseObject(ProfileConstants.FALLBACK_STUDENT_PROFILE);

        assertNotNull(json.getJSONObject("professional_skills").getBigDecimal("score"));
        assertNotNull(json.getJSONObject("certificates").getBigDecimal("score"));
        assertNotNull(json.getJSONObject("internship_experience").getBigDecimal("score"));
        assertNotNull(json.getJSONObject("overall_assessment").getBigDecimal("total_score"));
    }

    @Test
    @DisplayName("ProfileConstants 不允许实例化")
    void profileConstantsShouldNotBeInstantiated() {
        assertThrows(Exception.class, () -> {
            // 通过反射尝试实例化
            var constructor = ProfileConstants.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
    }
}
