package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.ParticipantDetailSnapshot
import com.harunmaul.epelatihan.domain.model.ParticipantListSnapshot
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateParticipantUseCaseTest {

    private class FakeAuthRepository(session: UserSession) : AuthRepository {
        private val sessionFlow = MutableStateFlow(session)
        override fun observeSession(): Flow<UserSession> = sessionFlow
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = null
    }

    private class FakeParticipantRepository : ParticipantRepository {
        var updatedParticipant: Participant? = null
        var updatedBy: String? = null

        override fun observeParticipants(includeInactive: Boolean): Flow<AppResult<ParticipantListSnapshot>> =
            flowOf(AppResult.Success(ParticipantListSnapshot(emptyList())))

        override fun observeParticipant(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>> =
            flowOf(AppResult.Success(ParticipantDetailSnapshot(null)))

        override suspend fun getParticipant(participantId: String): AppResult<Participant?> =
            AppResult.Success(null)

        override suspend fun findParticipantByPhone(phoneNormalized: String): AppResult<Participant?> =
            AppResult.Success(null)

        override suspend fun createParticipant(participant: Participant): AppResult<Participant> =
            AppResult.Success(participant)

        override suspend fun updateParticipant(actorId: String, participant: Participant): AppResult<Participant> {
            updatedBy = actorId
            updatedParticipant = participant
            return AppResult.Success(participant)
        }

        override suspend fun archiveParticipant(actorId: String, participantId: String): AppResult<Unit> =
            AppResult.Success(Unit)
    }

    @Test
    fun `regular user cannot update participant`() = runBlocking {
        val userProfile = UserProfile(
            id = "user-1",
            displayName = "User",
            email = "user@test.com",
            role = UserRole.USER,
            active = true,
            invitationStatus = InvitationStatus.ACTIVE,
            createdBy = "admin-1",
            createdAt = Instant.now(),
            updatedBy = "admin-1",
            updatedAt = Instant.now(),
        )
        val authRepo = FakeAuthRepository(UserSession.Authenticated(userProfile))
        val participantRepo = FakeParticipantRepository()
        val useCase = UpdateParticipantUseCase(participantRepo, authRepo)

        val result = useCase("p-1", "Budi", "08123456789")
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `admin can update participant`() = runBlocking {
        val adminProfile = UserProfile(
            id = "admin-1",
            displayName = "Admin",
            email = "admin@test.com",
            role = UserRole.ADMIN,
            active = true,
            invitationStatus = InvitationStatus.ACTIVE,
            createdBy = "admin-1",
            createdAt = Instant.now(),
            updatedBy = "admin-1",
            updatedAt = Instant.now(),
        )
        val authRepo = FakeAuthRepository(UserSession.Authenticated(adminProfile))
        val participantRepo = FakeParticipantRepository()
        val useCase = UpdateParticipantUseCase(participantRepo, authRepo)

        val result = useCase("p-1", "Budi Santoso", "0812-9999-8888", "budi@test.com", "Catatan baru")
        assertTrue(result is AppResult.Success)
        assertEquals("admin-1", participantRepo.updatedBy)
        assertEquals("Budi Santoso", participantRepo.updatedParticipant?.name)
        assertEquals("6281299998888", participantRepo.updatedParticipant?.phoneNormalized)
    }
}
