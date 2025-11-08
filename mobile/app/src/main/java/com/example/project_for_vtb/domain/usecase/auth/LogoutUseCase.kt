package com.example.project_for_vtb.domain.usecase.auth

import com.example.project_for_vtb.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case для выхода из системы
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.logout()
    }
}



