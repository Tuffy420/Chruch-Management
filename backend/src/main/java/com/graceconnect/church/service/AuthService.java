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

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    private static class LoginAttemptInfo {
        int failedAttempts = 0;
        long lockedUntil = 0;
    }

    private final java.util.concurrent.ConcurrentHashMap<String, LoginAttemptInfo> attemptMap = new java.util.concurrent.ConcurrentHashMap<>();

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

    private void checkLockout(String email) {
        String key = email.toLowerCase().trim();
        LoginAttemptInfo info = attemptMap.get(key);
        if (info != null && info.lockedUntil > System.currentTimeMillis()) {
            long remainingMinutes = Math.max(1, (info.lockedUntil - System.currentTimeMillis()) / (60 * 1000));
            throw new IllegalArgumentException("Account temporarily locked due to multiple failed login attempts. Try again in " + remainingMinutes + " minute(s).");
        }
    }

    private void recordFailedAttempt(String email) {
        String key = email.toLowerCase().trim();
        LoginAttemptInfo info = attemptMap.computeIfAbsent(key, k -> new LoginAttemptInfo());
        info.failedAttempts++;
        if (info.failedAttempts >= MAX_FAILED_ATTEMPTS) {
            info.lockedUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS;
            info.failedAttempts = 0; // reset counter once locked
        }

        try {
            activityLogRepository.save(ActivityLog.builder()
                    .action("FAILED login attempt for: " + email + " (Consecutive failures: " + info.failedAttempts + ")")
                    .module("Security")
                    .performedBy("Anonymous")
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (Exception ignored) { }
    }

    private void resetFailedAttempts(String email) {
        attemptMap.remove(email.toLowerCase().trim());
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
        String email = request.getEmail().trim();
        checkLockout(email);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            recordFailedAttempt(email);
            throw e;
        }

        resetFailedAttempts(email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User details not found!"));

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action("User login successful: " + user.getFullName() + " (" + user.getRole() + ")")
                .module("Security")
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
