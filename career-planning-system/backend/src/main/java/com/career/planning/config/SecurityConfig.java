package com.career.planning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 安全配置
 *
 * 设计说明：
 * 本项目使用自定义 JwtAuthInterceptor（注册在 WebMvcConfig）进行认证，
 * 不依赖 Spring Security 的过滤器链做权限控制。
 * 因此这里需要：
 * 1. 禁用 CSRF（项目使用 JWT，无需 CSRF 保护）
 * 2. 禁用 Session（使用无状态 JWT）
 * 3. 放行所有请求（认证逻辑由 JwtAuthInterceptor 拦截器统一处理）
 *
 * 注意：Spring Security 必须保留，因为 pom.xml 中引入了 spring-boot-starter-security 依赖，
 * 如果不配置 SecurityFilterChain，Spring Security 默认会拦截所有请求并要求登录。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 禁用 CSRF（项目使用 JWT，无需 CSRF 保护）
        http.csrf(AbstractHttpConfigurer::disable);

        // 禁用 Session（使用无状态 JWT）
        http.sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // 配置请求授权：放行所有接口
        // 认证逻辑由 JwtAuthInterceptor 拦截器处理
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/**").permitAll()
        );

        return http.build();
    }
}
