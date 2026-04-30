package com.seuportfolio.cnab_processor.infrastructure.config;

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
    public OpenAPI cnabProcessorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CNAB Processor API")
                        .version("1.0.0")
                        .description("""
                                API REST para processamento de arquivos bancários CNAB 240/400 (FEBRABAN).
                                Suporta Banco do Brasil, Itaú e Bradesco com validação de dígitos verificadores
                                Módulo 10 e Módulo 11.
                                """)
                        .contact(new Contact()
                                .name("Portfolio — Spring Batch + Clean Architecture")
                                .url("https://github.com/RonnyGuilherme/cnab-processor"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("http://localhost:8080").description("Docker")
                ));
    }
}