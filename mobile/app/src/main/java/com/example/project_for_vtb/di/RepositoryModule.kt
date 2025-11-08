package com.example.project_for_vtb.di

import com.example.project_for_vtb.data.local.dao.AccountDao
import com.example.project_for_vtb.data.local.dao.BankDao
import com.example.project_for_vtb.data.local.dao.TransactionDao
import com.example.project_for_vtb.data.remote.api.BankApiService
import com.example.project_for_vtb.data.security.TokenManager
import com.example.project_for_vtb.domain.repository.AccountRepository
import com.example.project_for_vtb.domain.repository.AuthRepository
import com.example.project_for_vtb.domain.repository.BankRepository
import com.example.project_for_vtb.domain.repository.TransactionRepository
import com.example.project_for_vtb.repository.AccountRepositoryImpl
import com.example.project_for_vtb.repository.AuthRepositoryImpl
import com.example.project_for_vtb.repository.BankRepositoryImpl
import com.example.project_for_vtb.repository.TransactionRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Модуль для привязки репозиториев
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindBankRepository(
        bankRepositoryImpl: BankRepositoryImpl
    ): BankRepository
    
    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        accountRepositoryImpl: AccountRepositoryImpl
    ): AccountRepository
    
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository
}



