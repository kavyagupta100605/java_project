package com.keybridge.runner;

import com.keybridge.entity.Token;
import com.keybridge.entity.User;
import com.keybridge.enums.Role;
import com.keybridge.enums.TokenStatus;
import com.keybridge.repository.TokenRepository;
import com.keybridge.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, TokenRepository tokenRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Seeding default KeyBridge users and tokens...");

            User admin = new User(
                    "System Administrator",
                    "admin@keybridge.com",
                    passwordEncoder.encode("Admin@123"),
                    Role.ADMIN
            );

            User regularUser = new User(
                    "Standard User",
                    "user@keybridge.com",
                    passwordEncoder.encode("User@123"),
                    Role.USER
            );

            User auditor = new User(
                    "Compliance Auditor",
                    "auditor@keybridge.com",
                    passwordEncoder.encode("Audit@123"),
                    Role.AUDITOR
            );

            userRepository.saveAll(List.of(admin, regularUser, auditor));
            log.info("Default users seeded: admin@keybridge.com, user@keybridge.com, auditor@keybridge.com");

            // Seed 5 Tokens
            Token t1 = new Token("TKN-001", "Hardware Key 1");
            t1.setOwner(regularUser);
            t1.setStatus(TokenStatus.ASSIGNED);
            t1.setAssignedDate(LocalDate.now());

            Token t2 = new Token("TKN-002", "Hardware Key 2");
            Token t3 = new Token("TKN-003", "Hardware Key 3");
            Token t4 = new Token("TKN-004", "Software Token Alpha");
            Token t5 = new Token("TKN-005", "Software Token Beta");

            tokenRepository.saveAll(List.of(t1, t2, t3, t4, t5));
            log.info("Default tokens seeded: TKN-001 to TKN-005");
        }
    }
}
