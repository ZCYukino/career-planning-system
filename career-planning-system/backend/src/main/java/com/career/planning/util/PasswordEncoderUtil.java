package com.career.planning.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 密码加密工具类
 * 使用 Spring Security 的 BCrypt 算法，提供安全的密码哈希
 */
public final class PasswordEncoderUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private PasswordEncoderUtil() {
    }

    /**
     * 加密密码
     * 
     * @param rawPassword 明文密码
     * @return BCrypt加密后的密码
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isEmpty()) {
            return "";
        }
        return ENCODER.encode(rawPassword);
    }

    /**
     * 验证密码
     * 
     * @param rawPassword 明文密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return ENCODER.matches(rawPassword, encodedPassword);
    }
}
