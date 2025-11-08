package com.example.project_for_vtb.di

import android.content.Context
import androidx.room.Room
import com.example.project_for_vtb.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Модуль для предоставления Room Database
 * 
 * ВАЖНО: SQLCipher временно отключен для компиляции проекта.
 * После исправления проблем с зависимостями необходимо добавить шифрование:
 * 1. Добавить импорт: import net.zetetic.database.sqlcipher.SupportFactory
 * 2. Создать SupportFactory с passphrase
 * 3. Использовать .openHelperFactory(factory) в databaseBuilder
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        // TODO: Добавить SQLCipher шифрование после исправления зависимостей
        // val passphrase = "your-secret-passphrase".toByteArray()
        // val factory = SupportFactory(passphrase)
        
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app_database.db"
        )
            // TODO: Включить после добавления SQLCipher
            // .openHelperFactory(factory)
            .fallbackToDestructiveMigration() // TODO: Реализовать миграции
            .build()
    }
    
    @Provides
    fun provideBankDao(database: AppDatabase) = database.bankDao()
    
    @Provides
    fun provideAccountDao(database: AppDatabase) = database.accountDao()
    
    @Provides
    fun provideTransactionDao(database: AppDatabase) = database.transactionDao()
}

