package com.example.project_for_vtb.domain.model

import java.math.BigDecimal

/**
 * Модель счета в domain слое
 */
data class Account(
    val id: String,
    val bankId: String,
    val accountNumber: String,
    val accountType: AccountType,
    val balance: BigDecimal,
    val currency: String,
    val name: String? = null,
    val isActive: Boolean = true
)

enum class AccountType {
    CHECKING,      // Текущий счет
    SAVINGS,       // Сберегательный
    CREDIT,        // Кредитный
    DEBIT_CARD,    // Дебетовая карта
    CREDIT_CARD    // Кредитная карта
}



