package com.example.project_for_vtb.domain.model

/**
 * Модель банка в domain слое
 */
data class Bank(
    val id: String,
    val name: String,
    val logoUrl: String? = null,
    val authUrl: String,
    val tokenUrl: String,
    val clientId: String,
    val isConnected: Boolean = false,
    val connectedAt: Long? = null
)


