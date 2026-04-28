package com.career.planning.interceptor;

import com.career.planning.common.CommonResult;
import com.career.planning.util.JwtBlacklistUtil;
import com.career.planning.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 认证拦截器
 * 拦截所有 /api/auth/user/** 和 /api/auth/password 接口
 * 校验 Token 有效性 + 黑名单状态
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthInterceptor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtBlacklistUtil jwtBlacklistUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 仅拦截需要认证的接口
        String path = request.getRequestURI();

        // 获取 Authorization Header
        String authorization = request.getHeader("Authorization");

        // 提取 Token
        String token = extractToken(authorization);

        if (token == null) {
            log.warn("请求 {} 缺少 Token", path);
            sendUnauthorized(response, "请先登录");
            return false;
        }

        // 校验 Token 格式与有效期
        if (!jwtUtil.validateToken(token)) {
            log.warn("请求 {} Token 已过期: {}", path, token.substring(0, Math.min(20, token.length())));
            sendUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        // 校验 Token 黑名单
        if (jwtBlacklistUtil.isBlacklisted(token)) {
            log.warn("请求 {} Token 已在黑名单: {}", path, token.substring(0, Math.min(20, token.length())));
            sendUnauthorized(response, "登录已失效，请重新登录");
            return false;
        }

        // Token 有效，将 username 放入请求属性，供后续使用
        String username = jwtUtil.parseUsername(token);
        request.setAttribute("username", username);
        request.setAttribute("token", token);

        return true;
    }

    private String extractToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7).trim();
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK); // 返回 200，错误码在 body 中
        response.setContentType("application/json;charset=UTF-8");
        CommonResult<?> result = CommonResult.unauthorized(message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
