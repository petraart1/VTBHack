package com.bank.controller;

import com.bank.dto.*;
import com.bank.exception.BadCredentialsException;
import com.bank.service.AuthService;
import com.messenger.dto.*;
import com.bank.exception.UserAlreadyExistsException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Register request: {}", registerRequest);
        try {
            UserDto user = authService.register(registerRequest);
            log.info("User registered: {}", user);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (UserAlreadyExistsException e) {
            log.info("User already exists: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request: {}", loginRequest);
        try {
            LoginResponse response = authService.login(loginRequest);
            log.info("Login response: {}", response);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (BadCredentialsException e) {
            log.info("Bad credentials: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    ResponseEntity<?> logout(@Valid @RequestBody LogoutRequest logoutRequest) {
        log.info("Logout request: {}", logoutRequest);
        return null;
    }

    @PostMapping("/refresh")
    ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest refreshRequest) {
        log.info("Refresh request: {}", refreshRequest);
        return null;
    }
}
