package com.graceconnect.church.config;

import com.graceconnect.church.model.User;
import com.graceconnect.church.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .email("admin@graceconnect.org")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("System Administrator")
                    .role("ADMIN")
                    .build();
            userRepository.save(admin);
            System.out.println("==================================================================");
            System.out.println("Default admin user pre-seeded: admin@graceconnect.org / admin123");
            System.out.println("==================================================================");
        }
    }
}
