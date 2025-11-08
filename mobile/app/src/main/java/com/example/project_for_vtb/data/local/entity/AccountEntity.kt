package com.example.project_for_vtb.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Room entity для счета
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "bank_id")
    val bankId: String,
    @ColumnInfo(name = "account_number")
    val accountNumber: String,
    @ColumnInfo(name = "account_type")
    val accountType: String,
    val balance: String, // BigDecimal as String
    val currency: String,
    val name: String? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    fun getBalanceAsBigDecimal(): BigDecimal {
        return try {
            BigDecimal(balance)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }
}

