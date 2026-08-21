package com.oufeng.ecommerceuserprofile.infrastructure.config;

import com.oufeng.ecommerceuserprofile.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.DispatcherType;

/**
 * Spring Security 配置。
 * 系统采用无状态 JWT 认证，并通过 ROLE_USER、ROLE_ADMIN 区分普通用户与管理员。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /** BCrypt 密码编码器，数据库只保存不可逆密码摘要。 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 配置公开接口、认证规则、JWT 过滤器和统一 JSON 错误响应。 */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/api/v1/system/health", "/api/v1/auth/login",
                                "/api/v1/auth/register", "/api/v1/public/**").permitAll()
                        // 画像详情/列表、标签、指标、省份排名：仅普通用户（运营分析员）使用
                        // （Admin 运营数据总览仅需 /overview 与 /segments/distribution，放行见下方 authentic 兜底）
                        .requestMatchers(
                                "/api/v1/profiles/tags/**",
                                "/api/v1/profiles/metrics/**",
                                "/api/v1/profiles/province-ranking/**",
                                "/api/v1/profiles/users/**"
                        ).hasRole("USER")
                        // 业务分析与人群运营：仅普通用户（运营分析员）在 User 门户使用
                        .requestMatchers(
                                "/api/v1/admin/product-analysis/**",
                                "/api/v1/admin/repeat-analysis/**",
                                "/api/v1/admin/churn-analysis/**",
                                "/api/v1/admin/audience/**"
                        ).hasRole("USER")
                        // 聚类：Admin 用于「聚类重算」数据生产（Spark 作业），User 用于查看聚类结果
                        .requestMatchers("/api/v1/admin/cluster-analysis/**").hasAnyRole("ADMIN", "USER")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"code\":40100,\"message\":\"用户未登录或登录状态已失效\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write("{\"code\":40300,\"message\":\"没有权限执行该操作\"}");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
