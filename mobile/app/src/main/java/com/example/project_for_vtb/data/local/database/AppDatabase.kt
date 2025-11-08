package com.example.project_for_vtb.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.project_for_vtb.data.local.dao.AccountDao
import com.example.project_for_vtb.data.local.dao.BankDao
import com.example.project_for_vtb.data.local.dao.TransactionDao
import com.example.project_for_vtb.data.local.entity.AccountEntity
import com.example.project_for_vtb.data.local.entity.BankEntity
import com.example.project_for_vtb.data.local.entity.TransactionEntity

/**
 * Room Database с SQLCipher для шифрования
 */
@Database(
    entities = [
        BankEntity::class,
        AccountEntity::class,
        TransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters
abstract class AppDatabase : RoomDatabase() {
    abstract fun bankDao(): BankDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
}



