package de.othr.traintogether.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    //Only expose endpoints in the api package
    @Bean
    public GroupedOpenApi apiGroupByPackage() {
        return GroupedOpenApi.builder()
                .group("api")
                .packagesToScan("de.othr.traintogether.api")
                .build();
    }

    // Group only /api/** endpoints
    @Bean
    public GroupedOpenApi apiGroup() {
        return GroupedOpenApi.builder()
                .group("api")
                .pathsToMatch("/api/**")
                .build();
    }

    // Register a Bearer JWT security scheme
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("API").version("v1"))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
