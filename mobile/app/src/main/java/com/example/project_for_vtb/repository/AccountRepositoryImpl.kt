package com.example.project_for_vtb.repository

import com.example.project_for_vtb.data.local.dao.AccountDao
import com.example.project_for_vtb.data.mapper.AccountMapper
import com.example.project_for_vtb.data.remote.api.BankApiService
import com.example.project_for_vtb.data.security.TokenManager
import com.example.project_for_vtb.domain.model.Account
import com.example.project_for_vtb.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория для работы со счетами
 * Использует реальный BankApiService для получения данных из API
 */
@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val bankApiService: BankApiService,
    private val tokenManager: TokenManager
) : AccountRepository {
    
    override fun getAllAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { AccountMapper.entityToDomain(it) }
        }
    }
    
    override fun getAccountsByBank(bankId: String): Flow<List<Account>> {
        return accountDao.getAccountsByBank(bankId).map { entities ->
            entities.map { AccountMapper.entityToDomain(it) }
        }
    }
    
    override suspend fun getAccountById(accountId: String): Account? {
        return accountDao.getAccountById(accountId)?.let { AccountMapper.entityToDomain(it) }
    }
    
    override suspend fun syncAccounts(bankId: String): Result<Unit> {
        return try {
            Timber.d("🔄 [AccountRepository] Начало синхронизации счетов для банка: $bankId")
            Timber.d("🌐 [AccountRepository] Используется РЕАЛЬНЫЙ API: http://158.160.102.225/api/v1/bank/accounts")
            
            // Получаем токен для банка
            val accessToken = tokenManager.getAccessToken(bankId)
            if (accessToken == null) {
                Timber.e("❌ [AccountRepository] Токен доступа не найден для банка $bankId")
                return Result.failure<Unit>(Exception("Токен доступа не найден для банка $bankId"))
            }
            
            // Проверяем, не истек ли токен
            if (tokenManager.isTokenExpired(bankId)) {
                Timber.w("⚠️ [AccountRepository] Токен доступа истек для банка $bankId")
                return Result.failure<Unit>(Exception("Токен доступа истек. Необходимо обновить токен."))
            }
            
            Timber.d("✅ [AccountRepository] Токен найден, выполняется запрос к API...")
            
            // Запрашиваем счета из API
            val response = bankApiService.getAccounts("Bearer $accessToken")
            
            Timber.d("📡 [AccountRepository] Ответ получен. Код статуса: ${response.code()}")
            Timber.d("📡 [AccountRepository] URL запроса: ${response.raw().request.url}")
            
            if (response.isSuccessful && response.body() != null) {
                val accountsDto = response.body()!!.accounts
                Timber.i("✅ [AccountRepository] Успешно получено ${accountsDto.size} счетов из РЕАЛЬНОГО API")
                Timber.d("📊 [AccountRepository] Счета: ${accountsDto.map { it.accountNumber }}")
                
                val accounts = accountsDto.map { AccountMapper.dtoToEntity(it) }
                accountDao.insertAccounts(accounts)
                Timber.d("💾 [AccountRepository] Счета сохранены в локальную БД")
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Не удалось получить счета. Код ответа: ${response.code()}"
                Timber.e("❌ [AccountRepository] Ошибка API: $errorMessage")
                Timber.e("❌ [AccountRepository] Код ответа: ${response.code()}, Сообщение: $errorBody")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ [AccountRepository] Исключение при синхронизации счетов")
            Result.failure(e)
        }
    }
    
    override suspend fun getTotalBalance(): Result<Double> {
        return try {
            val balance = accountDao.getTotalBalance() ?: 0.0
            Result.success(balance)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

