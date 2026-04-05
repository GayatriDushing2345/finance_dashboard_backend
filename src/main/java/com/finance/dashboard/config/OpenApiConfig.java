package com.finance.dashboard.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * Access: http://localhost:8080/swagger-ui.html
 * API Docs: http://localhost:8080/api-docs
 *
 * The {@code bearerAuth} security scheme defined here is referenced by
 * {@code @SecurityRequirement(name = "bearerAuth")} on each controller,
 * enabling the Swagger UI "Authorize" button for token-based testing.
 */ 
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "Finance Dashboard API",
        version     = "1.0.0",
        description = "Production-ready REST API for personal and organisational finance management. " +
                      "Features JWT authentication, role-based access control (EMPLOYEE / MANAGER / ADMIN), " +
                      "an expense approval workflow, and monthly budget alerting.",
        contact     = @Contact(name = "Finance Dashboard Team")
    ),
    servers = @Server(url = "http://localhost:8080", description = "Local development server")
)
@SecurityScheme(
    name         = "bearerAuth",
    type         = SecuritySchemeType.HTTP,
    scheme       = "bearer",
    bearerFormat = "JWT",
    description  = "Paste your JWT token (without the 'Bearer ' prefix) to authorise all secured endpoints."
)
public class OpenApiConfig {
    // All configuration is supplied via annotations above.
}
