package com.example.project_for_vtb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.project_for_vtb.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы со счетами в Room
 */
@Dao
interface AccountDao {
    @Query("select * from accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>
    
    @Query("select * from accounts where bank_id = :bankId")
    fun getAccountsByBank(bankId: String): Flow<List<AccountEntity>>
    
    @Query("select * from accounts where id = :accountId")
    suspend fun getAccountById(accountId: String): AccountEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)
    
    @Query("delete from accounts where bank_id = :bankId")
    suspend fun deleteAccountsByBank(bankId: String)
    
    @Query("select sum(cast(balance as real)) from accounts where currency = :currency")
    suspend fun getTotalBalance(currency: String = "RUB"): Double?
}



