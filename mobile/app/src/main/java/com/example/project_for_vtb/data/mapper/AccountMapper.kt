package com.example.project_for_vtb.data.mapper

import com.example.project_for_vtb.data.dto.AccountDto
import com.example.project_for_vtb.data.local.entity.AccountEntity
import com.example.project_for_vtb.domain.model.Account
import java.math.BigDecimal

/**
 * Маппер для преобразования между DTO, Entity и Domain моделями Account
 */
object AccountMapper {
    fun dtoToDomain(dto: AccountDto): Account {
        return Account(
            id = dto.id,
            bankId = dto.bankId,
            accountNumber = dto.accountNumber,
            accountType = dto.toAccountType(),
            balance = BigDecimal(dto.balance),
            currency = dto.currency,
            name = dto.name,
            isActive = dto.isActive
        )
    }
    
    fun entityToDomain(entity: AccountEntity): Account {
        return Account(
            id = entity.id,
            bankId = entity.bankId,
            accountNumber = entity.accountNumber,
            accountType = when (entity.accountType.lowercase()) {
                "checking", "текущий" -> com.example.project_for_vtb.domain.model.AccountType.CHECKING
                "savings", "сберегательный" -> com.example.project_for_vtb.domain.model.AccountType.SAVINGS
                "credit", "кредитный" -> com.example.project_for_vtb.domain.model.AccountType.CREDIT
                "debit_card", "дебетовая карта" -> com.example.project_for_vtb.domain.model.AccountType.DEBIT_CARD
                "credit_card", "кредитная карта" -> com.example.project_for_vtb.domain.model.AccountType.CREDIT_CARD
                else -> com.example.project_for_vtb.domain.model.AccountType.CHECKING
            },
            balance = entity.getBalanceAsBigDecimal(),
            currency = entity.currency,
            name = entity.name,
            isActive = entity.isActive
        )
    }
    
    fun domainToEntity(account: Account): AccountEntity {
        return AccountEntity(
            id = account.id,
            bankId = account.bankId,
            accountNumber = account.accountNumber,
            accountType = account.accountType.name,
            balance = account.balance.toString(),
            currency = account.currency,
            name = account.name,
            isActive = account.isActive
        )
    }
    
    fun dtoToEntity(dto: AccountDto): AccountEntity {
        return AccountEntity(
            id = dto.id,
            bankId = dto.bankId,
            accountNumber = dto.accountNumber,
            accountType = dto.accountType,
            balance = dto.balance,
            currency = dto.currency,
            name = dto.name,
            isActive = dto.isActive
        )
    }
}



