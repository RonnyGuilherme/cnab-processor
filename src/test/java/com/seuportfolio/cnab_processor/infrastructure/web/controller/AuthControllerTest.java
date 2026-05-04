package com.seuportfolio.cnab_processor.infrastructure.web.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController — Login e JWT")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("Login com credenciais válidas retorna token")
    void deveRetornarTokenComCredenciaisValidas() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"username":"admin","password":"admin123"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    @DisplayName("Login com credenciais inválidas retorna 401")
    void deveRetornar401ComCredenciaisInvalidas() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"username":"admin","password":"wrong"}
                        """))
                .andExpect(status().isUnauthorized()); // ← era isForbidden()
    }

    @Test
    @DisplayName("Endpoint da API sem token retorna 401")
    void deveRetornar401SemToken() throws Exception {
        mockMvc.perform(post("/api/v1/cnab/upload"))
                .andExpect(status().isUnauthorized()); // ← era isForbidden()
    }

    @Test
    @DisplayName("Actuator health é público")
    void actuatorHealthEPublico() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
