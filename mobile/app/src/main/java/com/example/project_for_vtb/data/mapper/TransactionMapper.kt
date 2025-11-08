package com.example.project_for_vtb.data.mapper

import com.example.project_for_vtb.data.dto.TransactionDto
import com.example.project_for_vtb.data.local.entity.TransactionEntity
import com.example.project_for_vtb.domain.model.Transaction
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Маппер для преобразования между DTO, Entity и Domain моделями Transaction
 */
object TransactionMapper {
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    fun dtoToDomain(dto: TransactionDto): Transaction {
        return Transaction(
            id = dto.id,
            accountId = dto.accountId,
            bankId = dto.bankId,
            amount = BigDecimal(dto.amount),
            currency = dto.currency,
            type = dto.toTransactionType(),
            category = dto.category,
            description = dto.description,
            date = parseDate(dto.date),
            merchantName = dto.merchantName,
            reference = dto.reference
        )
    }
    
    fun entityToDomain(entity: TransactionEntity): Transaction {
        return Transaction(
            id = entity.id,
            accountId = entity.accountId,
            bankId = entity.bankId,
            amount = entity.getAmountAsBigDecimal(),
            currency = entity.currency,
            type = when (entity.type.lowercase()) {
                "income", "доход" -> com.example.project_for_vtb.domain.model.TransactionType.INCOME
                "expense", "расход" -> com.example.project_for_vtb.domain.model.TransactionType.EXPENSE
                "transfer", "перевод" -> com.example.project_for_vtb.domain.model.TransactionType.TRANSFER
                else -> com.example.project_for_vtb.domain.model.TransactionType.EXPENSE
            },
            category = entity.category,
            description = entity.description,
            date = Date(entity.date),
            merchantName = entity.merchantName,
            reference = entity.reference
        )
    }
    
    fun domainToEntity(transaction: Transaction): TransactionEntity {
        return TransactionEntity(
            id = transaction.id,
            accountId = transaction.accountId,
            bankId = transaction.bankId,
            amount = transaction.amount.toString(),
            currency = transaction.currency,
            type = transaction.type.name,
            category = transaction.category,
            description = transaction.description,
            date = transaction.date.time,
            merchantName = transaction.merchantName,
            reference = transaction.reference
        )
    }
    
    fun dtoToEntity(dto: TransactionDto): TransactionEntity {
        return TransactionEntity(
            id = dto.id,
            accountId = dto.accountId,
            bankId = dto.bankId,
            amount = dto.amount,
            currency = dto.currency,
            type = dto.type,
            category = dto.category,
            description = dto.description,
            date = parseDate(dto.date).time,
            merchantName = dto.merchantName,
            reference = dto.reference
        )
    }
    
    private fun parseDate(dateString: String): Date {
        return try {
            isoDateFormat.parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
}



