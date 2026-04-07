package com.leika.shop.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình CORS cho phép Frontend (VueJS, v.v...) gọi API
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Cho phép tất cả các endpoint
                .allowedOrigins("http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:8080") // Các origin frontend thường dùng
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // Cho phép gửi cookie và credentials
                .maxAge(3600); // Cache pre-flight request trong 1 giờ
    }
}
