package com.finance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Finance Dashboard API")
                        .version("1.0.0")
                        .description("""
                                ## Finance Data Processing & Access Control Backend
                                
                                A role-based finance dashboard API built with Spring Boot 3, JWT authentication, and H2.
                                
                                ### Quick Start
                                1. Call **POST /api/auth/login** with demo credentials below
                                2. Copy the `token` from the response
                                3. Click **Authorize** (top right), paste the token, click **Authorize**
                                4. All endpoints are now accessible based on your role
                                
                                ### Demo Credentials
                                | Role | Email | Password |
                                |------|-------|----------|
                                | ADMIN | admin@finance.com | Admin@123 |
                                | ANALYST | analyst@finance.com | Analyst@123 |
                                | VIEWER | viewer@finance.com | Viewer@123 |
                                
                                ### Role Permissions
                                | Action | VIEWER | ANALYST | ADMIN |
                                |--------|--------|---------|-------|
                                | View records & dashboard | ✅ | ✅ | ✅ |
                                | Create / Update records | ❌ | ✅ | ✅ |
                                | Delete records | ❌ | ❌ | ✅ |
                                | Manage users | ❌ | ❌ | ✅ |
                                """)
                        .contact(new Contact()
                                .name("Finance Backend")
                                .email("admin@finance.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT token here (without the 'Bearer ' prefix)")));
    }
}
