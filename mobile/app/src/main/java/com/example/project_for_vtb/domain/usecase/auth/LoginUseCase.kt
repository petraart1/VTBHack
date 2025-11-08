package com.example.project_for_vtb.domain.usecase.auth

import com.example.project_for_vtb.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Use case для выполнения OAuth2 логина
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        bankId: String,
        authUrl: String,
        tokenUrl: String,
        clientId: String,
        redirectUri: String
    ): Result<Unit> {
        return authRepository.loginWithOAuth2(
            bankId = bankId,
            authUrl = authUrl,
            tokenUrl = tokenUrl,
            clientId = clientId,
            redirectUri = redirectUri
        )
    }
}



