package com.c2certi.tms.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  OpenAPI tmsOpenApi() {
    return new OpenAPI()
        .info(new Info().title("Ticketing Management System API").version("1.0.0"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "sessionCookie",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("TMS_SESSION")))
        .addSecurityItem(new SecurityRequirement().addList("sessionCookie"));
  }
}
