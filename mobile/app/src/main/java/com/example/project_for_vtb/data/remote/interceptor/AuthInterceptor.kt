package com.example.project_for_vtb.data.remote.interceptor

import com.example.project_for_vtb.data.security.TokenManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Интерцептор для автоматической подстановки токена авторизации в заголовки запросов
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Получаем токен из URL или используем дефолтный банк
        val bankId = extractBankIdFromRequest(originalRequest) ?: "default"
        val accessToken = tokenManager.getAccessToken(bankId)
        
        // Если токен истек, пытаемся обновить его
        if (accessToken != null && tokenManager.isTokenExpired(bankId)) {
            // TODO: Реализовать автоматическое обновление токена
            // Пока просто возвращаем запрос без токена или с истекшим токеном
        }
        
        val requestBuilder = originalRequest.newBuilder()
        
        // Если токен найден и не истек, добавляем его в заголовок
        if (accessToken != null && !tokenManager.isTokenExpired(bankId)) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }
        
        return chain.proceed(requestBuilder.build())
    }
    
    /**
     * Извлекает bankId из запроса (можно добавить логику определения банка по URL)
     */
    private fun extractBankIdFromRequest(request: Request): String? {
        // TODO: Реализовать логику определения bankId из запроса
        // Например, из query параметров или из специального заголовка
        return null
    }
}


