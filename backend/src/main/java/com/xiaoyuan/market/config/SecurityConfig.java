package com.xiaoyuan.market.config;

import com.xiaoyuan.market.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 配置（无状态 + JWT）
 * PasswordEncoder 单独定义在 PasswordEncoderConfig，避免 SecurityConfig -> JwtAuthFilter -> UserService -> PasswordEncoder 的循环依赖
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 需登录的特例（需在 permitAll 之前声明）
                        .requestMatchers(HttpMethod.GET, "/products/my").authenticated()
                        // 公开接口
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/products/**", "/categories/**",
                                "/comments/**", "/stats/hot").permitAll()
                        .requestMatchers("/upload/**", "/favicon.ico", "/error").permitAll()
                        // 接口文档
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/doc.html", "/webjars/**").permitAll()
                        // 管理后台
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 其余均需登录
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) ->
                                com.xiaoyuan.market.common.JsonResponse.write(response, 401, "请先登录"))
                        .accessDeniedHandler((request, response, e) ->
                                com.xiaoyuan.market.common.JsonResponse.write(response, 403, "无权限访问")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
