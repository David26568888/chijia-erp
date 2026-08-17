package com.chijia.erp.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. 啟用跨域 CORS 配置
            .cors(Customizer.withDefaults())
            // 2. 開發階段暫時關閉 CSRF (避免 POST/PUT/DELETE 被擋)
            .csrf(csrf -> csrf.disable())
            // 3. 設定 API 請求權限
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll() // 開發階段允許 API 所有請求
                .anyRequest().authenticated()
            );

        return http.build();
    }

    // 💡 全局 CORS 設定：允許 React 前端 (Port 5173 / 3000) 存取
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 指定允許的前端網址 (不可用 "*"，否則帶憑證/Cookie會失敗)
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        
        // 允許的 HTTP 方法
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        // 允許所有 Header 標頭
        config.setAllowedHeaders(List.of("*"));
        
        // 允許跨域傳送 Cookie 或 Authorization 認證資訊
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 對所有路徑生效
        return source;
    }
}