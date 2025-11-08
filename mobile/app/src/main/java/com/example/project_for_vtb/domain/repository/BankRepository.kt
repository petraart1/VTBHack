package com.example.project_for_vtb.domain.repository

import com.example.project_for_vtb.domain.model.Bank
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с банками
 */
interface BankRepository {
    /**
     * Получить список всех доступных банков
     */
    fun getAvailableBanks(): Flow<List<Bank>>
    
    /**
     * Получить список подключенных банков
     */
    fun getConnectedBanks(): Flow<List<Bank>>
    
    /**
     * Подключить банк
     */
    suspend fun connectBank(bank: Bank): Result<Unit>
    
    /**
     * Отключить банк
     */
    suspend fun disconnectBank(bankId: String): Result<Unit>
    
    /**
     * Получить банк по ID
     */
    suspend fun getBankById(bankId: String): Bank?
}



