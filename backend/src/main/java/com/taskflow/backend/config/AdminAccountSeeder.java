package com.taskflow.backend.config;

import com.taskflow.backend.entity.Role;
import com.taskflow.backend.entity.User;
import com.taskflow.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates one ADMIN account at startup when {@code taskflow.admin.enabled} is true.
 *
 * <p>Registration over HTTP always produces a USER account — accepting a role
 * from the request body would let anyone make themselves an admin. Something has
 * to create the first admin, and that something is deliberately configuration,
 * not an API. Idempotent: does nothing if the username already exists.
 */
@Component
@ConditionalOnProperty(prefix = "taskflow.admin", name = "enabled", havingValue = "true")
public class AdminAccountSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public AdminAccountSeeder(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${taskflow.admin.username}") String username,
                              @Value("${taskflow.admin.email}") String email,
                              @Value("${taskflow.admin.password}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (password == null || password.isBlank()) {
            log.warn("taskflow.admin.enabled=true but no taskflow.admin.password was supplied; "
                    + "skipping admin seeding.");
            return;
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.warn("Admin email {} is already registered to another account; skipping admin seeding.", email);
            return;
        }

        userRepository.save(new User(username, email, passwordEncoder.encode(password), Role.ADMIN));
        log.info("Seeded ADMIN account '{}'. Change or disable this before exposing the app publicly.", username);
    }
}
