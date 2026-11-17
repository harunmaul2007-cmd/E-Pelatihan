package com.harunmaul.epelatihan.domain.repository

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUserProfile(uid: String): UserProfile?
    fun observeUserProfile(uid: String): Flow<UserProfile?>
    fun observeUsers(): Flow<List<UserProfile>>
    suspend fun hasAnyAdmin(): Boolean
    suspend fun bootstrapFirstAdmin(name: String, email: String, password: String): AppResult<UserProfile>
    suspend fun inviteUser(actorId: String, name: String, email: String, role: UserRole): AppResult<UserProfile>
    suspend fun resendInvitation(actorId: String, userId: String, email: String): AppResult<Unit>
    suspend fun updateUserRole(actorId: String, userId: String, newRole: UserRole): AppResult<Unit>
    suspend fun updateUserStatus(actorId: String, userId: String, active: Boolean): AppResult<Unit>
}
