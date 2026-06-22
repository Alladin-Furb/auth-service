package com.microservices.auth.config;

import com.microservices.auth.entity.Role;
import com.microservices.auth.entity.User;
import com.microservices.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public AuthDataInitializer(
            JdbcTemplate jdbcTemplate,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${bootstrap.admin.name}") String adminName,
            @Value("${bootstrap.admin.email}") String adminEmail,
            @Value("${bootstrap.admin.password}") String adminPassword) {
        this.jdbcTemplate = jdbcTemplate;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute(
                "ALTER TABLE users MODIFY COLUMN role VARCHAR(32) NOT NULL");
        jdbcTemplate.update(
                "UPDATE users SET role = 'ROLE_ALUNO' WHERE role = 'ROLE_USER'");

        userRepository.findByEmail(adminEmail).orElseGet(() ->
                userRepository.save(new User(
                        adminName,
                        adminEmail,
                        passwordEncoder.encode(adminPassword),
                        Role.ROLE_ADMIN,
                        null
                )));
    }
}
