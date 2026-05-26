package com.graceconnect.church.service;

import com.graceconnect.church.config.JwtUtil;
import com.graceconnect.church.dto.JwtResponse;
import com.graceconnect.church.dto.LoginRequest;
import com.graceconnect.church.dto.RegisterRequest;
import com.graceconnect.church.model.ActivityLog;
import com.graceconnect.church.model.User;
import com.graceconnect.church.repository.ActivityLogRepository;
import com.graceconnect.church.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final ActivityLogRepository activityLogRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager,
                       ActivityLogRepository activityLogRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered!");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole().toUpperCase())
                .build();

        User savedUser = userRepository.save(user);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action("New user registered: " + savedUser.getFullName() + " (" + savedUser.getRole() + ")")
                .module("System")
                .performedBy("System Admin")
                .timestamp(LocalDateTime.now())
                .build());

        return savedUser;
    }

    public JwtResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User details not found!"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action("User login successful: " + user.getFullName())
                .module("System")
                .performedBy(user.getFullName())
                .timestamp(LocalDateTime.now())
                .build());

        return JwtResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole())
                .fullName(user.getFullName())
                .build();
    }
}
