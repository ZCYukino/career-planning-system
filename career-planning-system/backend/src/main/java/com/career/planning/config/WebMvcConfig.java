package com.career.planning.config;

import com.career.planning.interceptor.JwtAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 注册 JWT 认证拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                // 拦截需要 Token 认证的接口
                .addPathPatterns("/api/auth/user/**")
                .addPathPatterns("/api/auth/password")
                // 排除无需认证的接口
                .excludePathPatterns("/api/auth/register")
                .excludePathPatterns("/api/auth/login")
                .excludePathPatterns("/api/student/**")  // 原有学生信息接口（暂时保留兼容）
                .excludePathPatterns("/api/job/**")
                .excludePathPatterns("/api/career-path/**")
                .excludePathPatterns("/api/report/**")
                .excludePathPatterns("/api/knowledge/**")
                .excludePathPatterns("/api/data/**");
    }
}
