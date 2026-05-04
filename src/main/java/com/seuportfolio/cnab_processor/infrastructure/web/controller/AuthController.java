package com.seuportfolio.cnab_processor.infrastructure.web.controller;

import com.seuportfolio.cnab_processor.infrastructure.security.JwtUtil;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.LoginRequest;
import com.seuportfolio.cnab_processor.infrastructure.web.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login e obtenção de token JWT")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;

    @Value("${app.security.jwt.expiration-ms:86400000}")
    private long expirationMs;

    @PostMapping("/login")
    @Operation(summary = "Autentica e retorna token JWT",
            description = "Credenciais: admin/admin123 (ADMIN) ou api_user/user123 (USER)")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password())
        );

        String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(a -> a.replace("ROLE_", ""))
                .orElse("USER");

        String token = jwtUtil.generateToken(auth.getName(), role);
        return ResponseEntity.ok(LoginResponse.bearer(token, expirationMs));
    }
}