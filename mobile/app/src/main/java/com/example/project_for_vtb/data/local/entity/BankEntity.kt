package com.example.project_for_vtb.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity для банка
 */
@Entity(tableName = "banks")
data class BankEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    @ColumnInfo(name = "logo_url")
    val logoUrl: String? = null,
    @ColumnInfo(name = "auth_url")
    val authUrl: String,
    @ColumnInfo(name = "token_url")
    val tokenUrl: String,
    @ColumnInfo(name = "client_id")
    val clientId: String,
    @ColumnInfo(name = "is_connected")
    val isConnected: Boolean = false,
    @ColumnInfo(name = "connected_at")
    val connectedAt: Long? = null
)

