package com.example.locator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /**
     * Builds the HTTP security rules for the application.
     *
     * @param http Spring Security builder for the servlet filter chain
     * @param enabled controls whether HTTP Basic authentication protects API endpoints
     * @return configured stateless security filter chain
     * @throws Exception when Spring Security cannot build the filter chain
     */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${app.security.enabled:false}") boolean enabled
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (enabled) {
            log.info("HTTP Basic security is enabled for /api/**");
            http.authorizeHttpRequests(a -> a
                            .requestMatchers(
                                    "/",
                                    "/index.html",
                                    "/favicon.ico",
                                    "/*.js",
                                    "/*.css",
                                    "/assets/**",
                                    "/actuator/health/**"
                            ).permitAll()
                            .requestMatchers("/api/**").authenticated()
                            .anyRequest().permitAll())
                    .httpBasic(Customizer.withDefaults());
        } else {
            log.warn("Application security is disabled; all requests are permitted");
            http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
        }

        return http.build();
    }

    /**
     * Creates the in-memory API user used by HTTP Basic authentication.
     *
     * @param username configured API username
     * @param password configured API password before hashing
     * @param encoder password encoder used before storing credentials in memory
     * @return user details service containing the single configured API user
     */
    @Bean
    UserDetailsService userDetailsService(
            @Value("${app.security.username:local-user}") String username,
            @Value("${app.security.password:local-password}") String password,
            PasswordEncoder encoder
    ) {
        log.info("Configuring in-memory API user '{}'", username);
        return new InMemoryUserDetailsManager(
                User.withUsername(username).password(encoder.encode(password)).roles("API").build()
        );
    }

    /**
     * Provides the encoder used to hash configured API passwords.
     *
     * @return BCrypt password encoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
