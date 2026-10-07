package com.example.attendance;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

        @Override
        public void addCorsMappings(CorsRegistry registry) {

                registry.addMapping("/**")
                                .allowedOrigins(
                                                "http://localhost:3000",
                                                "http://127.0.0.1:3000",
                                                "http://localhost:8080",
                                                "http://127.0.0.1:8080",
                                                "https://attendance-management-3d-webinar.vercel.app",
                                                "https://attendance-management-git-main-3d-webinar.vercel.app",
                                                "https://attendance-management-lhsosyu8t-3d-webinar.vercel.app",
                                                "https://attendance-management-nine-beige.vercel.app",
                                                "https://attendance-management-i8sv38ljw-3d-webinar.vercel.app",
                                                "https://attendance-management-gver1a1cb-3d-webinar.vercel.app")
                                .allowedMethods(
                                                "GET",
                                                "POST",
                                                "PUT",
                                                "DELETE",
                                                "OPTIONS")
                                .allowedHeaders("*")
                                .allowCredentials(false);
        }
}