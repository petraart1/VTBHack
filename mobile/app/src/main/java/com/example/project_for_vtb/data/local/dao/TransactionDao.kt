package com.example.project_for_vtb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.project_for_vtb.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с транзакциями в Room
 */
@Dao
interface TransactionDao {
    @Query("select * from transactions order by date desc")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("select * from transactions where account_id = :accountId order by date desc")
    fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>>
    
    @Query("select * from transactions where bank_id = :bankId order by date desc")
    fun getTransactionsByBank(bankId: String): Flow<List<TransactionEntity>>
    
    @Query("select * from transactions where date between :startDate and :endDate order by date desc")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    
    @Query("select * from transactions where description like '%' || :query || '%' order by date desc")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)
    
    @Query("delete from transactions where bank_id = :bankId")
    suspend fun deleteTransactionsByBank(bankId: String)
}



