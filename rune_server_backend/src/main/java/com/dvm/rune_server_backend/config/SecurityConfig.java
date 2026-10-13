package com.dvm.rune_server_backend.config;

import com.dvm.rune_server_backend.common.Code;
import com.dvm.rune_server_backend.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * @author 25826
 * 构造自己的安全放行内容，并用@EnableWebSecurity表示替代原有默认配置
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
             // 只对该对应路径的请求生效，剩余的全部放行
             .securityMatcher("/api/**")
             // 处理跨域请求，允许前端跨域访问后端端口
             .cors(cors -> cors.configurationSource(corsConfigurationSource()))
             // 关闭csrf防护，防止POST/PUT/DELETE请求被拦截
             .csrf(AbstractHttpConfigurer::disable)
             // 无状态session管理，不使用session
             .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
             // 配置放行规则
             .authorizeHttpRequests(auth -> auth
                     // /api/auth/ 开头的路径全部放行，无需登录
                     .requestMatchers("/api/auth/**").permitAll()
                     // 剩余的 /api/** 请求都要登录
                     .anyRequest().authenticated()
             )
             // token 缺失/过期/无效 → 403报错，退出登录
             .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                 response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                 response.setContentType("application/json;charset=UTF-8");
                 response.getWriter().write(objectMapper.writeValueAsString(Result.error(Code.LOGIN_TIME_OUT)));
             }))
             // 在 Spring 自己的认证过滤器之前，插入我们的 JWT 过滤器
             .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // 跨域请求处理
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许所有来源的跨域请求
        config.setAllowedOriginPatterns(List.of("*"));
        // 允许常用的请求方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头，如 Authorization、Content-Type
        config.setAllowedHeaders(List.of("*"));
        // 允许携带 Cookie / 认证信息
        config.setAllowCredentials(true);
        // 预检请求的有效期（秒），即浏览器每小时询问一次
        config.setMaxAge(3600L);
        // 注册：哪些路径应用这个 CORS 规则
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return source;
    }

    // BCrypt 加密
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
