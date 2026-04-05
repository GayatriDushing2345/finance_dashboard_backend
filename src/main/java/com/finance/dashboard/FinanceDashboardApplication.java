package com.finance.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Finance Dashboard Application entry point.
 *
 * {@code @EnableJpaAuditing} activates {@link org.springframework.data.annotation.CreatedDate}
 * and {@link org.springframework.data.annotation.LastModifiedDate} on JPA entities.
 */
@SpringBootApplication
@EnableJpaAuditing
public class FinanceDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceDashboardApplication.class, args);
    }
}
