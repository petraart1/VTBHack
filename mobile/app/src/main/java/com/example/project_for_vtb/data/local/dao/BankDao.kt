package com.example.project_for_vtb.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.project_for_vtb.data.local.entity.BankEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с банками в Room
 */
@Dao
interface BankDao {
    @Query("select * from banks")
    fun getAllBanks(): Flow<List<BankEntity>>
    
    @Query("select * from banks where is_connected = 1")
    fun getConnectedBanks(): Flow<List<BankEntity>>
    
    @Query("select * from banks where id = :bankId")
    suspend fun getBankById(bankId: String): BankEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: BankEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanks(banks: List<BankEntity>)
    
    @Query("update banks set is_connected = :isConnected, connected_at = :connectedAt where id = :bankId")
    suspend fun updateBankConnection(bankId: String, isConnected: Boolean, connectedAt: Long?)
}



