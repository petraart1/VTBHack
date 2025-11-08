package com.example.project_for_vtb.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Room entity для транзакции
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "bank_id")
    val bankId: String,
    val amount: String, // BigDecimal as String
    val currency: String,
    val type: String,
    val category: String? = null,
    val description: String,
    val date: Long, // Timestamp
    @ColumnInfo(name = "merchant_name")
    val merchantName: String? = null,
    val reference: String? = null,
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    fun getAmountAsBigDecimal(): BigDecimal {
        return try {
            BigDecimal(amount)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }
}

