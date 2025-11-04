package com.bank.security;

import com.bank.entity.model.Role;
import com.bank.entity.model.User;
import com.bank.repository.UserRepository;
import com.bank.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GithubSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // email может быть скрыт; берём либо email, либо делаем fallback
        String email = Optional.ofNullable((String) oAuth2User.getAttributes().get("email"))
                .orElseGet(() -> oAuth2User.getAttribute("login") + "@github.local");

        String name = Optional.ofNullable((String) oAuth2User.getAttributes().get("name"))
                .orElse("GitHub User");

        // найти/создать локального пользователя
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setPassword("{noop}"); // пароль не нужен для OAuth2
            u.setFirstName(name);
            u.setLastName("");
            u.setBirthOfDate(LocalDate.of(1970, 1, 1)); // плейсхолдер
            u.setRole(Role.CLIENT);
            return userRepository.save(u);
        });

        // выдать JWT с ролью
        String token = jwtService.generateAccessToken(user.getEmail(), List.of(user.getRole().name()));

        // вернуть JSON (или сделай редирект на фронт)
        try {
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                {"token":"%s"}
                """.formatted(token));
        } catch (Exception ignored) {}
    }
}
