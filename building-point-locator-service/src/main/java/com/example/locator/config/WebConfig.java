package com.example.locator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebConfig.class);

    private final String[] allowedOrigins;

    /**
     * Reads the comma-separated CORS origin list from configuration.
     *
     * @param allowedOrigins configured browser origins allowed to call the API
     */
    public WebConfig(@Value("${app.cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split(",");
        LOGGER.info("Configured CORS allowed origins: {}", String.join(",", this.allowedOrigins));
    }

    /**
     * Registers CORS rules for API routes so configured frontend origins can call them.
     *
     * @param registry Spring MVC CORS registry to update
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }

}
