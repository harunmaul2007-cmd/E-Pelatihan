package com.harunmaul.epelatihan.domain.repository

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeSession(): Flow<UserSession>
    suspend fun login(email: String, password: String): AppResult<UserProfile>
    suspend fun logout()
    suspend fun sendPasswordReset(email: String): AppResult<Unit>
    suspend fun getCurrentUserProfile(): UserProfile?
}
