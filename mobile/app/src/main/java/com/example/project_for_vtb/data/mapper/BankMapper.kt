package com.example.project_for_vtb.data.mapper

import com.example.project_for_vtb.data.local.entity.BankEntity
import com.example.project_for_vtb.domain.model.Bank

/**
 * Маппер для преобразования между Entity и Domain моделями Bank
 */
object BankMapper {
    fun entityToDomain(entity: BankEntity): Bank {
        return Bank(
            id = entity.id,
            name = entity.name,
            logoUrl = entity.logoUrl,
            authUrl = entity.authUrl,
            tokenUrl = entity.tokenUrl,
            clientId = entity.clientId,
            isConnected = entity.isConnected,
            connectedAt = entity.connectedAt
        )
    }
    
    fun domainToEntity(bank: Bank): BankEntity {
        return BankEntity(
            id = bank.id,
            name = bank.name,
            logoUrl = bank.logoUrl,
            authUrl = bank.authUrl,
            tokenUrl = bank.tokenUrl,
            clientId = bank.clientId,
            isConnected = bank.isConnected,
            connectedAt = bank.connectedAt
        )
    }
}



