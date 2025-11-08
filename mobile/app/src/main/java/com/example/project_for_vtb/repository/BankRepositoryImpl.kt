package com.example.project_for_vtb.repository

import com.example.project_for_vtb.data.local.dao.BankDao
import com.example.project_for_vtb.data.mapper.BankMapper
import com.example.project_for_vtb.domain.model.Bank
import com.example.project_for_vtb.domain.repository.BankRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Реализация репозитория для работы с банками
 */
@Singleton
class BankRepositoryImpl @Inject constructor(
    private val bankDao: BankDao
) : BankRepository {
    
    override fun getAvailableBanks(): Flow<List<Bank>> {
        return bankDao.getAllBanks().map { entities ->
            entities.map { BankMapper.entityToDomain(it) }
        }
    }
    
    override fun getConnectedBanks(): Flow<List<Bank>> {
        return bankDao.getConnectedBanks().map { entities ->
            entities.map { BankMapper.entityToDomain(it) }
        }
    }
    
    override suspend fun connectBank(bank: Bank): Result<Unit> {
        return try {
            bankDao.insertBank(BankMapper.domainToEntity(bank.copy(
                isConnected = true,
                connectedAt = System.currentTimeMillis()
            )))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun disconnectBank(bankId: String): Result<Unit> {
        return try {
            bankDao.updateBankConnection(bankId, isConnected = false, connectedAt = null)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getBankById(bankId: String): Bank? {
        return bankDao.getBankById(bankId)?.let { BankMapper.entityToDomain(it) }
    }
}



