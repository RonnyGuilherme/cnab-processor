package com.seuportfolio.cnab_processor.infrastructure.web.dto;

public record LoginResponse(String token, String type, long expiresInMs) {
    public static LoginResponse bearer(String token, long expiresInMs) {
        return new LoginResponse(token, "Bearer", expiresInMs);
    }
}
