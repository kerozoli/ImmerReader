package com.keroleap.immerreader;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.findByUsername("admin").isPresent()) {
            User adminUser = new User(
                    "admin",
                    passwordEncoder.encode("admin123"),
                    true
            );
            userRepository.save(adminUser);
        }
    }
}
