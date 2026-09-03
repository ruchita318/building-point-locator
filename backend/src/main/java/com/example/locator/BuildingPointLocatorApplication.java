package com.example.locator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class BuildingPointLocatorApplication {
    private static final Logger log = LoggerFactory.getLogger(BuildingPointLocatorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BuildingPointLocatorApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logStartup(ApplicationReadyEvent event) {
        Environment environment = event.getApplicationContext().getEnvironment();
        String port = environment.getProperty("server.port", "8080");
        String profiles = String.join(",", environment.getActiveProfiles());
        log.info("Building Point Locator started on port {} with active profiles [{}]", port, profiles);
    }
}
