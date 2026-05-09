package com.cism.backend.config.cors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {
    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = properties.allowedOrigins().stream()
                .flatMap(origin -> Arrays.stream(origin.split(",")))
                .map(String::trim)
                .toList();

        logger.info("Configuring CORS with allowed origins: {}", origins);

        config.setAllowedOrigins(origins);
        
        List<String> methods = properties.allowedMethods().stream()
                .flatMap(method -> Arrays.stream(method.split(",")))
                .map(String::trim)
                .toList();
        config.setAllowedMethods(methods);

        config.setAllowedHeaders(List.of("*"));

        config.setAllowCredentials(properties.allowCredentials());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
