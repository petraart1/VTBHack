package com.bank.service;

import com.bank.entity.dto.*;
import com.bank.entity.model.Role;
import com.bank.entity.model.User;
import com.bank.exception.UserAlreadyExistsException;
import com.bank.repository.UserRepository;
import com.bank.security.SecurityUser;
import com.bank.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwtService;

    @Override
    public Response_RegisterDTO register(Request_RegisterDTO dto) {
        if (repository.findByEmail(dto.email()).isPresent()) {
            throw new UserAlreadyExistsException("User already exists");
        }
        User user = new User();
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setBirthOfDate(dto.birthOfDate());
        user.setEmail(dto.email());
        user.setPassword(encoder.encode(dto.password()));
        user.setRole(Role.CLIENT);

        repository.save(user);
        return new Response_RegisterDTO();
    }

    @Override
    public Response_LoginDTO login(Request_LoginDTO dto) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password())
        );

        var roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        // Получаем userId из базы данных
        User user = repository.findByEmail(dto.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtService.generateAccessToken(dto.email(), user.getUuid().toString(), roles);
        return new Response_LoginDTO(token);
    }
}
