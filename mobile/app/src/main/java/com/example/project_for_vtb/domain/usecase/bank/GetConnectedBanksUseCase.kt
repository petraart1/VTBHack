package com.example.project_for_vtb.domain.usecase.bank

import com.example.project_for_vtb.domain.model.Bank
import com.example.project_for_vtb.domain.repository.BankRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case для получения подключенных банков
 */
class GetConnectedBanksUseCase @Inject constructor(
    private val bankRepository: BankRepository
) {
    operator fun invoke(): Flow<List<Bank>> {
        return bankRepository.getConnectedBanks()
    }
}



