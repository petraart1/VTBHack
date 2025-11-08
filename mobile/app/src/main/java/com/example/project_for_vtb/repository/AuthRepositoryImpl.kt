package com.example.project_for_vtb.repository

import com.example.project_for_vtb.data.remote.api.AuthApiService
import com.example.project_for_vtb.data.remote.api.RefreshTokenRequest
import com.example.project_for_vtb.data.remote.api.TokenExchangeRequest
import com.example.project_for_vtb.data.security.TokenManager
import com.example.project_for_vtb.domain.model.AuthState
import com.example.project_for_vtb.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория аутентификации с использованием реального AuthApiService
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val tokenManager: TokenManager,
    private val authApiService: AuthApiService
) : AuthRepository {
    
    // TODO: Получить из конфигурации или BuildConfig
    private val clientId = "" // TODO: Настроить client_id для OAuth2
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override fun getAuthState(): Flow<AuthState> = _authState.asStateFlow()
    
    override suspend fun loginWithOAuth2(
        bankId: String,
        authUrl: String,
        tokenUrl: String,
        clientId: String,
        redirectUri: String
    ): Result<Unit> {
        return try {
            Timber.d("🔐 [AuthRepository] Начало OAuth2 авторизации для банка: $bankId")
            Timber.w("⚠️ [AuthRepository] OAuth2 flow требует полной реализации через AppAuth")
            Timber.w("⚠️ [AuthRepository] Необходимо использовать AuthorizationService для получения authorization code")
            
            _authState.value = AuthState.Loading("Выполняется авторизация...")
            
            // TODO: Реализовать полный OAuth2 flow с PKCE через AppAuth
            // Это упрощенная версия, полная реализация требует Activity для redirect
            // После получения authorization code, вызвать exchangeCodeForToken
            
            // ВРЕМЕННАЯ ЗАГЛУШКА - НЕ ИСПОЛЬЗУЕТ РЕАЛЬНЫЙ API!
            Timber.e("❌ [AuthRepository] ВНИМАНИЕ: Используется заглушка авторизации!")
            Timber.e("❌ [AuthRepository] Авторизация НЕ выполняется через реальный API!")
            
            _authState.value = AuthState.Authenticated(bankId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ [AuthRepository] Ошибка OAuth2 авторизации")
            _authState.value = AuthState.Error(e.message ?: "Ошибка авторизации")
            Result.failure(e)
        }
    }
    
    /**
     * Обмен authorization code на access token
     */
    suspend fun exchangeCodeForToken(
        bankId: String,
        code: String,
        redirectUri: String,
        codeVerifier: String
    ): Result<Unit> {
        return try {
            Timber.d("🔐 [AuthRepository] Начало обмена кода на токен для банка: $bankId")
            Timber.d("🌐 [AuthRepository] Используется РЕАЛЬНЫЙ API: http://158.160.102.225/api/v1/auth/token")
            
            val response = authApiService.exchangeCodeForToken(
                TokenExchangeRequest(
                    code = code,
                    redirect_uri = redirectUri,
                    client_id = clientId,
                    code_verifier = codeVerifier,
                    grant_type = "authorization_code"
                )
            )
            
            Timber.d("📡 [AuthRepository] Ответ получен. Код статуса: ${response.code()}")
            Timber.d("📡 [AuthRepository] URL запроса: ${response.raw().request.url}")
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                Timber.i("✅ [AuthRepository] Успешно получен токен из РЕАЛЬНОГО API")
                Timber.d("🔑 [AuthRepository] Токен будет действителен ${authResponse.expiresIn} секунд")
                
                tokenManager.saveTokens(
                    bankId = bankId,
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken,
                    expiresIn = authResponse.expiresIn
                )
                _authState.value = AuthState.Authenticated(bankId)
                Result.success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Не удалось получить токен"
                Timber.e("❌ [AuthRepository] Ошибка получения токена: $errorMessage")
                Timber.e("❌ [AuthRepository] Код ответа: ${response.code()}")
                _authState.value = AuthState.Error(errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ [AuthRepository] Исключение при получении токена")
            _authState.value = AuthState.Error(e.message ?: "Ошибка получения токена")
            Result.failure(e)
        }
    }
    
    override suspend fun refreshToken(bankId: String): Result<Unit> {
        return try {
            Timber.d("🔄 [AuthRepository] Обновление токена для банка: $bankId")
            Timber.d("🌐 [AuthRepository] Используется РЕАЛЬНЫЙ API: http://158.160.102.225/api/v1/auth/refresh")
            
            val refreshToken = tokenManager.getRefreshToken(bankId)
            if (refreshToken == null) {
                Timber.e("❌ [AuthRepository] Refresh token не найден для банка $bankId")
                return Result.failure<Unit>(Exception("Refresh token не найден"))
            }
            
            val response = authApiService.refreshToken(
                RefreshTokenRequest(
                    refresh_token = refreshToken,
                    client_id = clientId,
                    grant_type = "refresh_token"
                )
            )
            
            Timber.d("📡 [AuthRepository] Ответ получен. Код статуса: ${response.code()}")
            
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                Timber.i("✅ [AuthRepository] Токен успешно обновлен из РЕАЛЬНОГО API")
                
                tokenManager.saveTokens(
                    bankId = bankId,
                    accessToken = authResponse.accessToken,
                    refreshToken = authResponse.refreshToken,
                    expiresIn = authResponse.expiresIn
                )
                Result.success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Не удалось обновить токен"
                Timber.e("❌ [AuthRepository] Ошибка обновления токена: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ [AuthRepository] Исключение при обновлении токена")
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            tokenManager.clearAllTokens()
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isAuthenticated(): Boolean {
        // Проверяем наличие хотя бы одного валидного токена
        val defaultToken = tokenManager.getAccessToken("default")
        return defaultToken != null && !tokenManager.isTokenExpired("default")
    }
    
    override suspend fun getAccessToken(bankId: String): String? {
        // Если токен истек, пытаемся обновить его
        if (tokenManager.isTokenExpired(bankId)) {
            refreshToken(bankId).getOrNull()
        }
        return tokenManager.getAccessToken(bankId)
    }
}


