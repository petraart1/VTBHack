package com.example.project_for_vtb.domain.repository

import com.example.project_for_vtb.domain.model.AuthState
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для аутентификации
 */
interface AuthRepository {
    /**
     * Получить текущее состояние аутентификации
     */
    fun getAuthState(): Flow<AuthState>
    
    /**
     * Выполнить OAuth2 авторизацию через Authorization Code + PKCE
     */
    suspend fun loginWithOAuth2(
        bankId: String,
        authUrl: String,
        tokenUrl: String,
        clientId: String,
        redirectUri: String
    ): Result<Unit>
    
    /**
     * Обновить access token используя refresh token
     */
    suspend fun refreshToken(bankId: String): Result<Unit>
    
    /**
     * Выйти из системы
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * Проверить, авторизован ли пользователь
     */
    suspend fun isAuthenticated(): Boolean
    
    /**
     * Получить access token для банка
     */
    suspend fun getAccessToken(bankId: String): String?
}



