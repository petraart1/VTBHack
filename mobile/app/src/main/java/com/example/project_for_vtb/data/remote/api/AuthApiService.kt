package com.example.project_for_vtb.data.remote.api

import com.example.project_for_vtb.data.dto.AuthResponseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API сервис для работы с авторизацией
 * Базовый URL: http://158.160.102.225/api/v1/auth
 */
interface AuthApiService {
    /**
     * Авторизация по email и паролю
     * POST /api/v1/auth/login
     * 
     * ВАЖНО: Согласно API_CONTRACT.md, этот endpoint может быть не реализован на сервере.
     * Сервер использует OAuth2 flow, который требует:
     * 1. Получение authorization code через OAuth2 (браузер)
     * 2. Обмен code на токен через /auth/token
     * 
     * Этот метод оставлен для совместимости, но может возвращать HTML редирект на OAuth2.
     * Используем ResponseBody чтобы избежать ошибок парсинга.
     */
    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ResponseBody>
    
    /**
     * OAuth2 авторизация (Authorization Code + PKCE)
     * POST /api/v1/auth/token
     */
    @POST("token")
    suspend fun exchangeCodeForToken(
        @Body request: TokenExchangeRequest
    ): Response<AuthResponseDto>
    
    /**
     * Обновление токена
     * POST /api/v1/auth/refresh
     */
    @POST("refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponseDto>
    
    /**
     * Регистрация нового пользователя
     * POST /api/v1/auth/register
     * Возвращает пустой объект {} при успехе (200 OK)
     * При ошибке может возвращать строку или JSON с ошибкой
     * Используем ResponseBody чтобы избежать ошибок парсинга
     */
    @POST("register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ResponseBody>
}

/**
 * Запрос на обмен кода на токен
 */
data class TokenExchangeRequest(
    val code: String,
    val redirect_uri: String,
    val client_id: String,
    val code_verifier: String,
    val grant_type: String = "authorization_code"
)

/**
 * Запрос на авторизацию по email и паролю
 */
data class LoginRequest(
    val email: String,
    val password: String
)

/**
 * Запрос на обновление токена
 */
data class RefreshTokenRequest(
    val refresh_token: String,
    val client_id: String,
    val grant_type: String = "refresh_token"
)

/**
 * Запрос на регистрацию нового пользователя
 * Соответствует API контракту: POST /auth/register
 */
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val birthOfDate: String, // Формат: "YYYY-MM-DD"
    val email: String,
    val password: String
)

