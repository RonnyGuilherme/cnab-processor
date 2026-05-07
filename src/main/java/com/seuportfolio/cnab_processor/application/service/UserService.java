package com.seuportfolio.cnab_processor.application.service;

import com.seuportfolio.cnab_processor.domain.model.User;
import com.seuportfolio.cnab_processor.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de gestão de usuários.
 *
 * <p>Centraliza operações de criação e atualização de usuários,
 * garantindo que senhas sempre sejam armazenadas com BCrypt.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cria um usuário se ele ainda não existir.
     * Usado pelo {@link DataInitializer} no startup.
     */
    @Transactional
    public void createIfAbsent(String username, String rawPassword, String role) {
        if (userRepository.existsByUsername(username)) {
            log.debug("Usuário '{}' já existe — pulando criação.", username);
            return;
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Usuário '{}' criado com role '{}'.", username, role);
    }

    @Transactional
    public void changePassword(String username, String newRawPassword) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setPassword(passwordEncoder.encode(newRawPassword));
            userRepository.save(user);
            log.info("Senha do usuário '{}' atualizada.", username);
        });
    }
}