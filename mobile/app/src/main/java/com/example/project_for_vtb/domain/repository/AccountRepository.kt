package com.example.project_for_vtb.domain.repository

import com.example.project_for_vtb.domain.model.Account
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы со счетами
 */
interface AccountRepository {
    /**
     * Получить все счета пользователя
     */
    fun getAllAccounts(): Flow<List<Account>>
    
    /**
     * Получить счета по банку
     */
    fun getAccountsByBank(bankId: String): Flow<List<Account>>
    
    /**
     * Получить счет по ID
     */
    suspend fun getAccountById(accountId: String): Account?
    
    /**
     * Синхронизировать счета с сервером
     */
    suspend fun syncAccounts(bankId: String): Result<Unit>
    
    /**
     * Получить общий баланс всех счетов
     */
    suspend fun getTotalBalance(): Result<Double>
}



