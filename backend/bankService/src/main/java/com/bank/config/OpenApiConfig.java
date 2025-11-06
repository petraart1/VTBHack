package com.bank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Конфигурация OpenAPI (Swagger) для автоматической генерации документации API
 * Документация доступна по адресу: http://localhost:8081/swagger-ui.html
 * OpenAPI JSON: http://localhost:8081/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bankServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank Service API")
                        .description("REST API для управления банковскими счетами, транзакциями и балансами. " +
                                "Часть микросервисной архитектуры RadarSubscriptions для агрегации данных из Open Banking API.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("RadarSubscriptions Team")
                                .email("support@radarsubscriptions.ru")
                                .url("https://radarsubscriptions.ru"))
                        .license(new License()
                                .name("Private License")
                                .url("https://radarsubscriptions.ru/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Development Server"),
                        new Server()
                                .url("http://localhost/api/v1/bank")
                                .description("Production (через Nginx)")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT токен от authService. Формат: Authorization: Bearer {token}")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}

