package com.seuportfolio.cnab_processor.infrastructure.config;

import com.seuportfolio.cnab_processor.application.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Inicializa os usuários padrão do sistema no startup.
 *
 * <p>Executa após o contexto Spring estar completamente carregado,
 * garantindo que o {@link com.seuportfolio.cnab_processor.infrastructure.security.CustomUserDetailsService}
 * e o {@link org.springframework.security.crypto.password.PasswordEncoder} já estejam disponíveis.</p>
 *
 * <p>Em produção, as senhas devem ser configuradas via variáveis de ambiente
 * {@code ADMIN_PASSWORD} e {@code USER_PASSWORD}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserService userService;

    @Value("${app.security.users.admin-password:admin123}")
    private String adminPassword;

    @Value("${app.security.users.user-password:user123}")
    private String userPassword;

    @Override
    public void run(ApplicationArguments args) {
        log.info("DataInitializer — verificando usuários padrão...");
        userService.createIfAbsent("admin",    adminPassword, "ADMIN");
        userService.createIfAbsent("api_user", userPassword,  "USER");
        log.info("DataInitializer — concluído.");
    }
}