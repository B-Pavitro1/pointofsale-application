package com.pos;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@Slf4j
public class PosApplication {

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    public void validateStartupConfig() {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32) {
            log.error("CRITICAL: 'app.jwt.secret' is missing, empty, or too short (minimum 32 characters required)!");
            throw new IllegalStateException("Security startup failure: 'app.jwt.secret' must be at least 32 characters long.");
        }
        log.info("Application startup configuration verified successfully.");
    }

    public static void main(String[] args) {
        SpringApplication.run(PosApplication.class, args);
    }
}
