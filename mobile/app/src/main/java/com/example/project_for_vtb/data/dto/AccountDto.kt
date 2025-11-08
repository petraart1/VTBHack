package com.example.project_for_vtb.data.dto

import com.example.project_for_vtb.domain.model.AccountType
import com.google.gson.annotations.SerializedName

/**
 * DTO для счета из API
 * 
 * ВАЖНО: Структура этого DTO должна соответствовать формату ответа от Backend API.
 * При изменении формата ответа на Backend необходимо обновить данную модель.
 * 
 * Все поля помечены @SerializedName для соответствия JSON формату от Backend.
 */
data class AccountDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("bank_id")
    val bankId: String,
    @SerializedName("account_number")
    val accountNumber: String,
    @SerializedName("account_type")
    val accountType: String,
    @SerializedName("balance")
    val balance: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("is_active")
    val isActive: Boolean = true
) {
    fun toAccountType(): AccountType {
        return when (accountType.lowercase()) {
            "checking", "текущий" -> AccountType.CHECKING
            "savings", "сберегательный" -> AccountType.SAVINGS
            "credit", "кредитный" -> AccountType.CREDIT
            "debit_card", "дебетовая карта" -> AccountType.DEBIT_CARD
            "credit_card", "кредитная карта" -> AccountType.CREDIT_CARD
            else -> AccountType.CHECKING
        }
    }
}


