package com.example.project_for_vtb.domain.model

/**
 * Состояние аутентификации
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String) : AuthState()
    data class Loading(val message: String? = null) : AuthState()
    data class Error(val message: String) : AuthState()
}



