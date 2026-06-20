package com.vendra.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI/Swagger metadata. The exported spec (docs/openapi.json) is the shared client contract. */
@Configuration
public class OpenApiConfig {

  private static final String BEARER = "supabaseJwt";

  @Bean
  public OpenAPI vendraOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Vendra API")
                .version("0.1.0")
                .description(
                    "Multi-vendor marketplace API. Authenticate with Supabase Auth and call with"
                        + " `Authorization: Bearer <jwt>`. Roles: customer | shop | courier | admin.")
                .contact(new Contact().name("Vendra"))
                .license(new License().name("MIT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Supabase-issued JWT (verified via JWKS).")));
  }
}
