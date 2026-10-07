package com.pokerplanning.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pokerPlanningOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Poker Planning AI - API")
                .description("API REST pour la gestion des sessions de Planning Poker collaboratives assistées par Intelligence Artificielle.")
                .version("v1.0.0")
                .contact(new Contact()
                    .name("Poker Planning AI Team")
                    .email("support@pokerplanning.ai"))
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Environnement Local")
            ));
    }
}
