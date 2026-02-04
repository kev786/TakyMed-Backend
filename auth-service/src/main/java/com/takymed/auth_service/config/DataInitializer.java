package com.takymed.auth_service.config;

import com.takymed.auth_service.entities.User;
import com.takymed.auth_service.entities.enums.Role;
import com.takymed.auth_service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_LASTNAME}")
    private String adminLastName;

    @Value("${ADMIN_FIRSTNAME}")
    private String adminFirstName;

    @Value("${ADMIN_EMAIL}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (Boolean.FALSE.equals(userRepository.existsByEmail(adminEmail))) {
            log.info("Initialisation de l'administrateur par défaut : {}", adminEmail);

            User admin = User.builder()
                    .lastName(adminLastName)
                    .firstName(adminFirstName)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
            log.info("Administrateur par défaut créé avec succès.");
        } else {
            log.info("L'administrateur par défaut ({}) existe déjà.", adminEmail);
        }
    }
}
