package com.dvm.rune_server_backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * @author 25826
 * 每次 http 请求到达后端，都会先经过这个过滤器，检查请求头中是否包含 token
 * 如果有 token，就解析出来，让 Spring 知道访问者的身份
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        // 确认令牌存在且没有过期
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            // 将用户信息包装成 Spring Security 的认证对象，存入安全上下文中，之后 Controller 就可以知道谁在请求
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // 后继的放行由 SecurityConfig 检测，这里统一放行
        filterChain.doFilter(request, response);
    }

    // 从请求头取出 "Bearer <token>"，截取后面的 token 部分
    // 前端在请求头发送 token 的格式：Authorization: Bearer eyJhbG……（前缀 + JWT令牌本身）
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            // "Bearer " 共7个字符
            return header.substring(7);
        }
        return null;
    }
}
