package com.bank.service;

import com.bank.dto.*;
import com.bank.model.User;
import com.messenger.dto.*;
import com.bank.exception.UserAlreadyExistsException;
import com.bank.repository.UserRepository;
import com.bank.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserDto register(RegisterRequest request) {
        log.debug("Starting registration process for username: {}", request.username());

        if (userRepository.existsByUsername(request.username())) {
            log.warn("Username already taken: {}", request.username());
            throw new UserAlreadyExistsException("Username already taken: " + request.username());
        }

        if (userRepository.existsByEmail(request.email())) {
            log.warn("Email already registered: {}", request.email());
            throw new UserAlreadyExistsException("Email already registered: " + request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .build();

        User savedUser = userRepository.save(user);

        log.info("User created successfully - ID: {}, username: {}",
                savedUser.getId(), savedUser.getUsername());

        return new UserDto(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getCreatedAt()
        );
    }

    public LoginResponse login(LoginRequest request) {
        String identifier = request.username() != null ? request.username() : request.email();
        log.debug("Starting login process for identifier: {}", request.username());

        User user = findUserByIdentifier(request)
                .orElseThrow(() -> {
                    log.warn("User not found for identifier: {}", identifier);
                    return new BadCredentialsException("Invalid username or password");
                });

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Invalid password for user: {}", request.username());
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getUsername(),
                user.getEmail()
        );

        log.info("User authenticated successfully: {}", user.getUsername());

        return new LoginResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                token,
                "Bearer"
        );
    }

    public LogoutResponse logout(LogoutRequest request) {
        log.debug("Starting logout");
        return new LogoutResponse();
    }

    public RefreshResponse refresh(RefreshRequest request) {
        log.debug("Starting refresh");
        return new RefreshResponse();
    }

    private Optional<User> findUserByIdentifier(LoginRequest request) {
        if (request.username() != null && !request.username().isBlank()) {
            return userRepository.findByUsername(request.username());
        } else if (request.email() != null && !request.email().isBlank()) {
            return userRepository.findByEmail(request.email());
        }
        throw new IllegalArgumentException("No valid identifier provided");
    }

}
