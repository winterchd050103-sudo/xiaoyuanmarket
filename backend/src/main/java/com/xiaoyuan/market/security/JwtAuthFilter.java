package com.xiaoyuan.market.security;

import com.xiaoyuan.market.common.JsonResponse;
import com.xiaoyuan.market.entity.User;
import com.xiaoyuan.market.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 登录拦截过滤器
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = header.substring(7);
        try {
            Claims claims = jwtUtil.parse(token);
            Long userId = Long.valueOf(claims.getSubject());
            // 读 Redis 缓存（未命中回源数据库），同时保证禁用状态即时生效
            User user = userService.getByIdCached(userId);
            if (user == null || user.getStatus() == null || user.getStatus() == 0) {
                JsonResponse.write(response, 401, "账号已被禁用或不存在");
                return;
            }
            LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRole(), user.getStatus());
            var authorities = List.of(new SimpleGrantedAuthority(
                    loginUser.isAdmin() ? "ROLE_ADMIN" : "ROLE_USER"));
            var authentication = new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            JsonResponse.write(response, 401, "登录已过期，请重新登录");
        }
    }
}
