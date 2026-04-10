package com.taskhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 白名单：登录、注册、Swagger、WebSocket
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/error").permitAll()

                // ====== 管理员专属操作 ======
                // 项目管理（创建、删除）
                .requestMatchers(HttpMethod.POST, "/api/projects").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/projects/**").hasRole("ADMIN")
                // 项目成员管理
                .requestMatchers(HttpMethod.POST, "/api/projects/*/members").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/projects/*/members/**").hasRole("ADMIN")
                // 任务删除
                .requestMatchers(HttpMethod.DELETE, "/api/tasks/**").hasRole("ADMIN")
                // 甘特图依赖关系管理
                .requestMatchers(HttpMethod.POST, "/api/gantt/links").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/gantt/links").hasRole("ADMIN")
                // 智能排程
                .requestMatchers(HttpMethod.POST, "/api/tasks/reorder").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/tasks/batch-schedule").hasRole("ADMIN")
                // 里程碑管理
                .requestMatchers(HttpMethod.POST, "/api/milestones").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/milestones/**").hasRole("ADMIN")
                // 成员角色管理
                .requestMatchers(HttpMethod.PUT, "/api/members/**").hasRole("ADMIN")

                // ====== 认证用户可用（读 + 普通写） ======
                .requestMatchers("/api/**").authenticated()

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
