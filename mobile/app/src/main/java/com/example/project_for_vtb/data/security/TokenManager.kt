package com.example.project_for_vtb.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер для безопасного хранения OAuth2 токенов
 */
@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "encrypted_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    companion object {
        private const val KEY_ACCESS_TOKEN_PREFIX = "access_token_"
        private const val KEY_REFRESH_TOKEN_PREFIX = "refresh_token_"
        private const val KEY_EXPIRES_AT_PREFIX = "expires_at_"
    }
    
    /**
     * Сохранить токены для банка
     */
    fun saveTokens(
        bankId: String,
        accessToken: String,
        refreshToken: String?,
        expiresIn: Long
    ) {
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
        sharedPreferences.edit()
            .putString("${KEY_ACCESS_TOKEN_PREFIX}$bankId", accessToken)
            .putString("${KEY_REFRESH_TOKEN_PREFIX}$bankId", refreshToken)
            .putLong("${KEY_EXPIRES_AT_PREFIX}$bankId", expiresAt)
            .apply()
    }
    
    /**
     * Получить access token для банка
     */
    fun getAccessToken(bankId: String): String? {
        return sharedPreferences.getString("${KEY_ACCESS_TOKEN_PREFIX}$bankId", null)
    }
    
    /**
     * Получить refresh token для банка
     */
    fun getRefreshToken(bankId: String): String? {
        return sharedPreferences.getString("${KEY_REFRESH_TOKEN_PREFIX}$bankId", null)
    }
    
    /**
     * Проверить, не истек ли токен
     */
    fun isTokenExpired(bankId: String): Boolean {
        val expiresAt = sharedPreferences.getLong("${KEY_EXPIRES_AT_PREFIX}$bankId", 0)
        return expiresAt <= System.currentTimeMillis()
    }
    
    /**
     * Удалить токены для банка
     */
    fun clearTokens(bankId: String) {
        sharedPreferences.edit()
            .remove("${KEY_ACCESS_TOKEN_PREFIX}$bankId")
            .remove("${KEY_REFRESH_TOKEN_PREFIX}$bankId")
            .remove("${KEY_EXPIRES_AT_PREFIX}$bankId")
            .apply()
    }
    
    /**
     * Удалить все токены
     */
    fun clearAllTokens() {
        sharedPreferences.edit().clear().apply()
    }
}


