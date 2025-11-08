package com.example.project_for_vtb.domain.repository

import com.example.project_for_vtb.domain.model.Transaction
import java.util.Date
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с транзакциями
 */
interface TransactionRepository {
    /**
     * Получить все транзакции
     */
    fun getAllTransactions(): Flow<List<Transaction>>
    
    /**
     * Получить транзакции по счету
     */
    fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>>
    
    /**
     * Получить транзакции по банку
     */
    fun getTransactionsByBank(bankId: String): Flow<List<Transaction>>
    
    /**
     * Получить транзакции за период
     */
    fun getTransactionsByDateRange(
        startDate: Date,
        endDate: Date
    ): Flow<List<Transaction>>
    
    /**
     * Поиск транзакций по описанию
     */
    fun searchTransactions(query: String): Flow<List<Transaction>>
    
    /**
     * Синхронизировать транзакции с сервером
     */
    suspend fun syncTransactions(bankId: String, accountId: String? = null): Result<Unit>
}



