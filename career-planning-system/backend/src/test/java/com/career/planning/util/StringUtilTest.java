package com.career.planning.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StringUtil 工具类测试
 * 验证判空、转换、验证等核心方法
 */
class StringUtilTest {

    // ========== 基础判空 ==========

    @Test
    @DisplayName("isNull 正确判断")
    void testIsNull() {
        assertTrue(StringUtil.isNull(null));
        assertFalse(StringUtil.isNull(""));
        assertFalse(StringUtil.isNull("abc"));
    }

    @Test
    @DisplayName("isEmpty 正确判断")
    void testIsEmpty() {
        assertTrue(StringUtil.isEmpty(null));
        assertTrue(StringUtil.isEmpty(""));
        assertFalse(StringUtil.isEmpty(" "));
        assertFalse(StringUtil.isEmpty("abc"));
    }

    @Test
    @DisplayName("isBlank 正确判断（包含空白字符串）")
    void testIsBlank() {
        assertTrue(StringUtil.isBlank(null));
        assertTrue(StringUtil.isBlank(""));
        assertTrue(StringUtil.isBlank("   "));
        assertTrue(StringUtil.isBlank("\t\n"));
        assertFalse(StringUtil.isBlank("abc"));
        assertFalse(StringUtil.isBlank(" abc "));
    }

    @Test
    @DisplayName("isNotBlank 正确判断")
    void testIsNotBlank() {
        assertFalse(StringUtil.isNotBlank(null));
        assertFalse(StringUtil.isNotBlank(""));
        assertFalse(StringUtil.isNotBlank("   "));
        assertTrue(StringUtil.isNotBlank("abc"));
    }

    // ========== 转换方法 ==========

    @Test
    @DisplayName("defaultIfBlank 正确返回默认值")
    void testDefaultIfBlank() {
        assertEquals("默认", StringUtil.defaultIfBlank(null, "默认"));
        assertEquals("默认", StringUtil.defaultIfBlank("", "默认"));
        assertEquals("默认", StringUtil.defaultIfBlank("  ", "默认"));
        assertEquals("原值", StringUtil.defaultIfBlank("原值", "默认"));
    }

    @Test
    @DisplayName("emptyIfNull 正确处理")
    void testEmptyIfNull() {
        assertEquals("", StringUtil.emptyIfNull(null));
        assertEquals("abc", StringUtil.emptyIfNull("abc"));
    }

    @Test
    @DisplayName("trim 安全处理 null")
    void testTrim() {
        assertNull(StringUtil.trim(null));
        assertEquals("abc", StringUtil.trim(" abc "));
        assertEquals("", StringUtil.trim(""));
    }

    // ========== 验证方法 ==========

    @Test
    @DisplayName("isNumeric 正确判断数字字符串")
    void testIsNumeric() {
        assertTrue(StringUtil.isNumeric("123"));
        assertTrue(StringUtil.isNumeric("0"));
        assertFalse(StringUtil.isNumeric(null));
        assertFalse(StringUtil.isNumeric(""));
        assertFalse(StringUtil.isNumeric("12.3"));
        assertFalse(StringUtil.isNumeric("12a"));
    }

    @Test
    @DisplayName("isValidEmail 正确判断邮箱")
    void testIsValidEmail() {
        assertTrue(StringUtil.isValidEmail("test@example.com"));
        assertTrue(StringUtil.isValidEmail("user.name@domain.org"));
        assertFalse(StringUtil.isValidEmail(null));
        assertFalse(StringUtil.isValidEmail(""));
        assertFalse(StringUtil.isValidEmail("invalid"));
        assertFalse(StringUtil.isValidEmail("@no-user.com"));
    }

    @Test
    @DisplayName("isInvalidValue 正确识别无效值")
    void testIsInvalidValue() {
        assertTrue(StringUtil.isInvalidValue(null));
        assertTrue(StringUtil.isInvalidValue(""));
        assertTrue(StringUtil.isInvalidValue("null"));
        assertTrue(StringUtil.isInvalidValue("NULL"));
        assertTrue(StringUtil.isInvalidValue("undefined"));
        assertFalse(StringUtil.isInvalidValue("正常值"));
    }

    // ========== equals 方法 ==========

    @Test
    @DisplayName("equals null安全比较")
    void testEquals() {
        assertTrue(StringUtil.equals(null, null));
        assertTrue(StringUtil.equals("abc", "abc"));
        assertFalse(StringUtil.equals(null, "abc"));
        assertFalse(StringUtil.equals("abc", null));
        assertFalse(StringUtil.equals("abc", "ABC"));
    }

    @Test
    @DisplayName("equalsIgnoreCase 忽略大小写比较")
    void testEqualsIgnoreCase() {
        assertTrue(StringUtil.equalsIgnoreCase("abc", "ABC"));
        assertTrue(StringUtil.equalsIgnoreCase(null, null));
        assertFalse(StringUtil.equalsIgnoreCase("abc", null));
    }
}
