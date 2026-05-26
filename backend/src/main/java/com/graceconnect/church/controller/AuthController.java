package com.graceconnect.church.controller;

import com.graceconnect.church.dto.ApiResponse;
import com.graceconnect.church.dto.JwtResponse;
import com.graceconnect.church.dto.LoginRequest;
import com.graceconnect.church.dto.RegisterRequest;
import com.graceconnect.church.model.User;
import com.graceconnect.church.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication Module", description = "Endpoints for administrator registration and JWT authentication.")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new administrative user", description = "Creates a new User account with standard roles (ADMIN, PASTOR, STAFF) and encrypts the password with BCrypt.")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and return JWT Access Token", description = "Verifies username and password, generates a JWT token, and returns user profile details.")
    public ResponseEntity<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse jwtResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", jwtResponse));
    }
}
