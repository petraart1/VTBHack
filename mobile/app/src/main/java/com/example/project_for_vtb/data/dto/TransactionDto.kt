package com.example.project_for_vtb.data.dto

import com.example.project_for_vtb.domain.model.TransactionType
import com.google.gson.annotations.SerializedName

/**
 * DTO для транзакции из API
 * 
 * ВАЖНО: Структура этого DTO должна соответствовать формату ответа от Backend API.
 * При изменении формата ответа на Backend необходимо обновить данную модель.
 * 
 * Все поля помечены @SerializedName для соответствия JSON формату от Backend.
 * Дата передается в формате ISO 8601 (например: "2024-01-15T10:30:00.000Z").
 */
data class TransactionDto(
    @SerializedName("id")
    val id: String,
    @SerializedName("account_id")
    val accountId: String,
    @SerializedName("bank_id")
    val bankId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("category")
    val category: String? = null,
    @SerializedName("description")
    val description: String,
    @SerializedName("date")
    val date: String, // ISO 8601 format
    @SerializedName("merchant_name")
    val merchantName: String? = null,
    @SerializedName("reference")
    val reference: String? = null
) {
    fun toTransactionType(): TransactionType {
        return when (type.lowercase()) {
            "income", "доход" -> TransactionType.INCOME
            "expense", "расход" -> TransactionType.EXPENSE
            "transfer", "перевод" -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
    }
}


