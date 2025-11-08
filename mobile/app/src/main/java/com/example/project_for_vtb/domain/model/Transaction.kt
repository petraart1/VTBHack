package com.example.project_for_vtb.domain.model

import java.math.BigDecimal
import java.util.Date

/**
 * Модель транзакции в domain слое
 */
data class Transaction(
    val id: String,
    val accountId: String,
    val bankId: String,
    val amount: BigDecimal,
    val currency: String,
    val type: TransactionType,
    val category: String? = null,
    val description: String,
    val date: Date,
    val merchantName: String? = null,
    val reference: String? = null
)

enum class TransactionType {
    INCOME,     // Доход
    EXPENSE,    // Расход
    TRANSFER    // Перевод
}



