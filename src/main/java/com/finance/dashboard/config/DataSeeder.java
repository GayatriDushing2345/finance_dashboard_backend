package com.finance.dashboard.config;

import com.finance.dashboard.entity.Category;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.Role;
import com.finance.dashboard.enums.UserStatus;
import com.finance.dashboard.repository.CategoryRepository;
import com.finance.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository     userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder    passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
        seedCategories();
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    private void seedUsers() {
        createUserIfAbsent("System Admin",     "admin@finance.com",    "Admin@1234",    Role.ADMIN);
        createUserIfAbsent("Finance Manager",  "manager@finance.com",  "Manager@1234",  Role.MANAGER);
        createUserIfAbsent("John Employee",    "employee@finance.com", "Employee@1234", Role.EMPLOYEE);
    }

    private void createUserIfAbsent(String name, String email, String rawPassword, Role role) {
        if (!userRepository.existsByEmail(email)) {
            User user = User.builder()
                    .name(name)
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
            log.info("Seeded user  →  {} ({})", email, role);
        }
    }

    // ── Categories ────────────────────────────────────────────────────────────

    private void seedCategories() {
        List<String> defaults = List.of(
                "Food & Dining",
                "Travel & Transport",
                "Office Supplies",
                "Software & Subscriptions",
                "Marketing",
                "Salaries",
                "Utilities",
                "Rent",
                "Healthcare",
                "Miscellaneous"
        );

        defaults.forEach(name -> {
            if (!categoryRepository.existsByNameIgnoreCase(name)) {
                categoryRepository.save(Category.builder().name(name).build());
                log.info("Seeded category → {}", name);
            }
        });
    }
}
