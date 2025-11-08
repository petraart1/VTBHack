package com.example.project_for_vtb.data.mock

import android.content.Context
import com.example.project_for_vtb.data.dto.AccountDto
import com.example.project_for_vtb.data.dto.TransactionDto
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock сервис для работы с тестовыми данными
 * Используется для разработки Frontend до подключения реального Backend API
 */
@Singleton
class MockDataService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    
    /**
     * Получить mock данные счетов
     */
    suspend fun getMockAccounts(): List<AccountDto> {
        // Имитация сетевой задержки
        delay(500)
        
        return try {
            val json = context.assets.open("mockResponses/accountsresponse.json")
                .bufferedReader().use { it.readText() }
            val responseType = object : TypeToken<Map<String, List<AccountDto>>>() {}.type
            val response: Map<String, List<AccountDto>> = gson.fromJson(json, responseType)
            response["accounts"] ?: emptyList()
        } catch (e: Exception) {
            // Fallback на хардкод данные, если файл не найден
            getHardcodedAccounts()
        }
    }
    
    /**
     * Получить mock данные транзакций
     */
    suspend fun getMockTransactions(): List<TransactionDto> {
        // Имитация сетевой задержки
        delay(500)
        
        return try {
            val json = context.assets.open("mockResponses/transactionsresponse.json")
                .bufferedReader().use { it.readText() }
            val responseType = object : TypeToken<Map<String, List<TransactionDto>>>() {}.type
            val response: Map<String, List<TransactionDto>> = gson.fromJson(json, responseType)
            response["transactions"] ?: emptyList()
        } catch (e: Exception) {
            // Fallback на хардкод данные, если файл не найден
            getHardcodedTransactions()
        }
    }
    
    /**
     * Fallback: хардкод данные счетов
     */
    private fun getHardcodedAccounts(): List<AccountDto> {
        return listOf(
            AccountDto(
                id = "acc_001",
                bankId = "bank_vtb",
                accountNumber = "40817810099910004312",
                accountType = "checking",
                balance = "125000.50",
                currency = "RUB",
                name = "Основной счет",
                isActive = true
            ),
            AccountDto(
                id = "acc_002",
                bankId = "bank_vtb",
                accountNumber = "40817810099910004313",
                accountType = "savings",
                balance = "500000.00",
                currency = "RUB",
                name = "Накопительный счет",
                isActive = true
            )
        )
    }
    
    /**
     * Fallback: хардкод данные транзакций
     */
    private fun getHardcodedTransactions(): List<TransactionDto> {
        return listOf(
            TransactionDto(
                id = "trx_001",
                accountId = "acc_001",
                bankId = "bank_vtb",
                amount = "-1500.00",
                currency = "RUB",
                type = "expense",
                category = "Продукты",
                description = "Покупка в магазине",
                date = "2024-01-15T10:30:00.000Z",
                merchantName = "Магнит",
                reference = "REF123456"
            ),
            TransactionDto(
                id = "trx_002",
                accountId = "acc_001",
                bankId = "bank_vtb",
                amount = "50000.00",
                currency = "RUB",
                type = "income",
                category = "Зарплата",
                description = "Зарплата за январь",
                date = "2024-01-10T08:00:00.000Z",
                merchantName = null,
                reference = "SAL202401"
            )
        )
    }
}


