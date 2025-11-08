package com.example.project_for_vtb.repository

import com.example.project_for_vtb.data.local.dao.TransactionDao
import com.example.project_for_vtb.data.mapper.TransactionMapper
import com.example.project_for_vtb.data.remote.api.BankApiService
import com.example.project_for_vtb.data.security.TokenManager
import com.example.project_for_vtb.domain.model.Transaction
import com.example.project_for_vtb.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория для работы с транзакциями
 * Использует реальный BankApiService для получения данных из API
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val bankApiService: BankApiService,
    private val tokenManager: TokenManager
) : TransactionRepository {
    
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    override fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions().map { entities ->
            entities.map { TransactionMapper.entityToDomain(it) }
        }
    }
    
    override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(accountId).map { entities ->
            entities.map { TransactionMapper.entityToDomain(it) }
        }
    }
    
    override fun getTransactionsByBank(bankId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByBank(bankId).map { entities ->
            entities.map { TransactionMapper.entityToDomain(it) }
        }
    }
    
    override fun getTransactionsByDateRange(
        startDate: Date,
        endDate: Date
    ): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(
            startDate.time,
            endDate.time
        ).map { entities ->
            entities.map { TransactionMapper.entityToDomain(it) }
        }
    }
    
    override fun searchTransactions(query: String): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(query).map { entities ->
            entities.map { TransactionMapper.entityToDomain(it) }
        }
    }
    
    override suspend fun syncTransactions(bankId: String, accountId: String?): Result<Unit> {
        return try {
            val apiEndpoint = if (accountId != null) {
                "http://158.160.102.225/api/v1/bank/accounts/$accountId/transactions"
            } else {
                "http://158.160.102.225/api/v1/bank/transactions"
            }
            
            Timber.d("🔄 [TransactionRepository] Начало синхронизации транзакций для банка: $bankId")
            if (accountId != null) {
                Timber.d("🔄 [TransactionRepository] Синхронизация транзакций для счета: $accountId")
            }
            Timber.d("🌐 [TransactionRepository] Используется РЕАЛЬНЫЙ API: $apiEndpoint")
            
            // Получаем токен для банка
            val accessToken = tokenManager.getAccessToken(bankId)
            if (accessToken == null) {
                Timber.e("❌ [TransactionRepository] Токен доступа не найден для банка $bankId")
                return Result.failure<Unit>(Exception("Токен доступа не найден для банка $bankId"))
            }
            
            // Проверяем, не истек ли токен
            if (tokenManager.isTokenExpired(bankId)) {
                Timber.w("⚠️ [TransactionRepository] Токен доступа истек для банка $bankId")
                return Result.failure<Unit>(Exception("Токен доступа истек. Необходимо обновить токен."))
            }
            
            Timber.d("✅ [TransactionRepository] Токен найден, выполняется запрос к API...")
            
            // Запрашиваем транзакции из API
            val response = if (accountId != null) {
                // Получаем транзакции для конкретного счета
                bankApiService.getTransactions(
                    token = "Bearer $accessToken",
                    accountId = accountId,
                    fromDate = null, // TODO: Добавить поддержку диапазона дат
                    toDate = null
                )
            } else {
                // Получаем все транзакции
                bankApiService.getAllTransactions(
                    token = "Bearer $accessToken",
                    fromDate = null, // TODO: Добавить поддержку диапазона дат
                    toDate = null
                )
            }
            
            Timber.d("📡 [TransactionRepository] Ответ получен. Код статуса: ${response.code()}")
            Timber.d("📡 [TransactionRepository] URL запроса: ${response.raw().request.url}")
            
            if (response.isSuccessful && response.body() != null) {
                val transactionsDto = response.body()!!.transactions
                Timber.i("✅ [TransactionRepository] Успешно получено ${transactionsDto.size} транзакций из РЕАЛЬНОГО API")
                Timber.d("📊 [TransactionRepository] Транзакции: ${transactionsDto.take(5).map { "${it.type}: ${it.amount} ${it.currency}" }}")
                
                val transactions = transactionsDto.map { TransactionMapper.dtoToEntity(it) }
                transactionDao.insertTransactions(transactions)
                Timber.d("💾 [TransactionRepository] Транзакции сохранены в локальную БД")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Не удалось получить транзакции. Код ответа: ${response.code()}"
                Timber.e("❌ [TransactionRepository] Ошибка API: $errorMessage")
                Timber.e("❌ [TransactionRepository] Код ответа: ${response.code()}, Сообщение: $errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ [TransactionRepository] Исключение при синхронизации транзакций")
            Result.failure(e)
        }
    }
    
    /**
     * Форматирует дату в ISO 8601 формат для API запросов
     */
    private fun formatDate(date: Date): String {
        return isoDateFormat.format(date)
    }
}

