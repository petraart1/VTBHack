package com.example.project_for_vtb.data.remote.api

import com.example.project_for_vtb.data.dto.AccountDto
import com.example.project_for_vtb.data.dto.TransactionDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * API сервис для работы с банковскими данными
 * Базовый URL: http://158.160.102.225/api/v1/bank
 */
interface BankApiService {
    /**
     * Получение списка счетов
     * GET /api/v1/bank/accounts
     */
    @GET("accounts")
    suspend fun getAccounts(
        @Header("Authorization") token: String
    ): Response<AccountsResponse>
    
    /**
     * Получение транзакций по счету
     * GET /api/v1/bank/accounts/{accountId}/transactions
     */
    @GET("accounts/{accountId}/transactions")
    suspend fun getTransactions(
        @Header("Authorization") token: String,
        @Path("accountId") accountId: String,
        @Query("from") fromDate: String? = null,
        @Query("to") toDate: String? = null
    ): Response<TransactionsResponse>
    
    /**
     * Получение всех транзакций
     * GET /api/v1/bank/transactions
     */
    @GET("transactions")
    suspend fun getAllTransactions(
        @Header("Authorization") token: String,
        @Query("from") fromDate: String? = null,
        @Query("to") toDate: String? = null
    ): Response<TransactionsResponse>
}

/**
 * Ответ со списком счетов
 */
data class AccountsResponse(
    val accounts: List<AccountDto>
)

/**
 * Ответ со списком транзакций
 */
data class TransactionsResponse(
    val transactions: List<TransactionDto>
)
