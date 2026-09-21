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