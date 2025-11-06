package com.bank.controller;

import com.bank.entity.dto.*;
import com.bank.entity.model.Role;
import com.bank.entity.model.User;
import com.bank.repository.UserRepository;
import com.bank.security.JwtService;
import com.bank.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API для аутентификации и авторизации пользователей")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

        /**
     * Регистрация нового пользователя
     */
    @Operation(
            summary = "Регистрация пользователя",
            description = "Создает нового пользователя в системе. Возвращает пустой ответ при успешной регистрации."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно зарегистрирован",
                    content = @Content(schema = @Schema(implementation = Response_RegisterDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверные данные запроса или пользователь с таким email уже существует",
                    content = @Content
            )
    })
    @PostMapping("/register")
    public ResponseEntity<Response_RegisterDTO> register(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для регистрации",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Request_RegisterDTO.class),
                            examples = @ExampleObject(
                                    name = "Пример запроса",
                                    value = """
                                            {
                                              "firstName": "Иван",
                                              "lastName": "Иванов",
                                              "birthOfDate": "1990-01-01",
                                              "email": "ivan@example.com",
                                              "password": "SecurePassword123!"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody Request_RegisterDTO dto) {
        return ResponseEntity.ok(authService.register(dto));
    }

        /**
     * Вход пользователя в систему
     */
    @Operation(
            summary = "Вход в систему",
            description = "Аутентифицирует пользователя по email и паролю. Возвращает JWT токен для последующих запросов."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешный вход, возвращает JWT токен",
                    content = @Content(
                            schema = @Schema(implementation = Response_LoginDTO.class),
                            examples = @ExampleObject(
                                    name = "Пример ответа",
                                    value = """
                                            {
                                              "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверный email или пароль",
                    content = @Content
            )
    })
    @PostMapping("/login")
    public ResponseEntity<Response_LoginDTO> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Учетные данные пользователя",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = Request_LoginDTO.class),
                            examples = @ExampleObject(
                                    name = "Пример запроса",
                                    value = """
                                            {
                                              "email": "ivan@example.com",
                                              "password": "SecurePassword123!"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody Request_LoginDTO dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

        /**
     * Редирект на OAuth2 провайдера
     */
    @Operation(
            summary = "Инициация OAuth2 авторизации",
            description = "Перенаправляет пользователя на страницу авторизации OAuth2 провайдера (например, GitHub). " +
                    "После успешной авторизации провайдер перенаправит на /auth/oauth2/success"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "302",
                    description = "Редирект на страницу авторизации провайдера"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неподдерживаемый провайдер",
                    content = @Content
            )
    })
    @GetMapping("/oauth2/authorize/{provider}")
    public void oauth2Authorize(
            @Parameter(
                    description = "Провайдер OAuth2 (например: github)",
                    required = true,
                    example = "github"
            )
            @PathVariable String provider,
            HttpServletResponse response) throws IOException {
        response.sendRedirect("/oauth2/authorization/" + provider);
    }

    /**
     * Callback после успешной OAuth2 авторизации
     */
    @Operation(
            summary = "OAuth2 callback",
            description = "Обрабатывает ответ от OAuth2 провайдера после успешной авторизации. " +
                    "Если пользователь не существует - создает его автоматически. Возвращает JWT токен."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная авторизация, возвращает JWT токен",
                    content = @Content(
                            schema = @Schema(implementation = Response_LoginDTO.class),
                            examples = @ExampleObject(
                                    name = "Пример ответа",
                                    value = """
                                            {
                                              "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Ошибка авторизации через OAuth2",
                    content = @Content
            )
    })
    @GetMapping("/oauth2/success")
    public ResponseEntity<Response_LoginDTO> oauth2Success(
            @Parameter(hidden = true) Authentication authentication) {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = Optional.ofNullable((String) oAuth2User.getAttributes().get("email"))
                .orElseGet(() -> oAuth2User.getAttribute("login") + "@github.local");

        String name = Optional.ofNullable((String) oAuth2User.getAttributes().get("name"))
                .orElse("GitHub User");

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

        String token = jwtService.generateAccessToken(user.getEmail(), user.getUuid().toString(), List.of(user.getRole().name()));
        return ResponseEntity.ok(new Response_LoginDTO(token));
    }
}