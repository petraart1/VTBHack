package com.example.project_for_vtb.domain.usecase.account

import com.example.project_for_vtb.domain.model.Account
import com.example.project_for_vtb.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения всех счетов
 */
class GetAllAccountsUseCase @Inject constructor(
    private val accountRepository: AccountRepository
) {
    operator fun invoke(): Flow<List<Account>> {
        return accountRepository.getAllAccounts()
    }
}



