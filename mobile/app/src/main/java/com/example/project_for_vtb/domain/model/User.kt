package com.example.project_for_vtb.domain.model

/**
 * Модель пользователя в domain слое
 */
data class User(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val isAuthenticated: Boolean = false
)



