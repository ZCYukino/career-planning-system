package com.career.planning.util;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 字符串判空工具类
 * 统一处理 null、空字符串、空白字符的判断
 */
public final class StringUtil {

    // 空白字符正则：空格、制表符、换行符等
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s*");

    private StringUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== 基础判空方法 ====================

    /**
     * 判断字符串是否为null
     */
    public static boolean isNull(String str) {
        return str == null;
    }

    /**
     * 判断字符串是否不为null
     */
    public static boolean isNotNull(String str) {
        return str != null;
    }

    /**
     * 判断字符串是否为null或空字符串("")
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * 判断字符串是否不为null且不为空字符串
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * 判断字符串是否为null、空字符串或仅包含空白字符
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 判断字符串是否不为null、且不为空字符串、且不仅包含空白字符
     */
    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    // ==================== 转换方法 ====================

    /**
     * 如果字符串为null，返回空字符串
     */
    public static String emptyIfNull(String str) {
        return str == null ? "" : str;
    }

    /**
     * 如果字符串为null或空白，返回空字符串
     */
    public static String emptyIfBlank(String str) {
        return isBlank(str) ? "" : str;
    }

    /**
     * 如果字符串为null或空白，返回默认值
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    /**
     * 如果字符串为null，返回默认值
     */
    public static String defaultIfNull(String str, String defaultValue) {
        return str != null ? str : defaultValue;
    }

    /**
     * 安全trim，如果为null返回null
     */
    public static String trim(String str) {
        return str != null ? str.trim() : null;
    }

    /**
     * 安全trim，如果为null或空白返回null
     */
    public static String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ==================== 验证方法 ====================

    /**
     * 判断字符串长度是否在指定范围内
     */
    public static boolean isLengthInRange(String str, int min, int max) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        return length >= min && length <= max;
    }

    /**
     * 判断字符串是否仅包含数字
     */
    public static boolean isNumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否仅包含字母
     */
    public static boolean isAlpha(String str) {
        if (isEmpty(str)) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isLetter(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否仅包含字母和数字
     */
    public static boolean isAlphanumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isLetterOrDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // ==================== 特殊场景判断 ====================

    /**
     * 判断字符串是否为"null"字符串（不是Java的null，而是字符串"null"）
     */
    public static boolean isNullString(String str) {
        return "null".equalsIgnoreCase(str);
    }

    /**
     * 判断字符串是否为"undefined"字符串
     */
    public static boolean isUndefinedString(String str) {
        return "undefined".equalsIgnoreCase(str);
    }

    /**
     * 判断字符串是否为无效值（null、"null"、"undefined"、空白）
     */
    public static boolean isInvalidValue(String str) {
        return isBlank(str) || isNullString(str) || isUndefinedString(str);
    }

    /**
     * 判断两个字符串是否相等（忽略大小写，null安全）
     */
    public static boolean equalsIgnoreCase(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.equalsIgnoreCase(str2);
    }

    /**
     * 判断两个字符串是否相等（null安全）
     */
    public static boolean equals(String str1, String str2) {
        if (str1 == null && str2 == null) {
            return true;
        }
        if (str1 == null || str2 == null) {
            return false;
        }
        return str1.equals(str2);
    }

    // ==================== 业务相关工具方法 ====================

    /**
     * 判断是否有效的姓名
     */
    public static boolean isValidName(String name) {
        return isNotBlank(name) && isLengthInRange(name, 1, 50);
    }

    /**
     * 判断是否有效的学号
     */
    public static boolean isValidStudentId(String studentId) {
        return isNotBlank(studentId) && isAlphanumeric(studentId) && isLengthInRange(studentId, 5, 20);
    }

    /**
     * 判断是否有效的手机号（中国大陆）
     */
    public static boolean isValidPhone(String phone) {
        if (isBlank(phone)) {
            return false;
        }
        return phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 判断是否有效的邮箱
     */
    public static boolean isValidEmail(String email) {
        if (isBlank(email)) {
            return false;
        }
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }
}
