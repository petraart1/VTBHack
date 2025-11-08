package com.example.project_for_vtb.data.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO для ответа OAuth2 токенов
 * 
 * ВАЖНО: Структура этого DTO должна соответствовать формату ответа от Backend API.
 * При изменении формата ответа на Backend необходимо обновить данную модель.
 * 
 * Поля помечены @SerializedName для соответствия JSON формату от Backend.
 */
data class AuthResponseDto(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String? = null,
    @SerializedName("token_type")
    val tokenType: String = "Bearer",
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("scope")
    val scope: String? = null
)


