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

class CreateParticipantUseCaseTest {

    private class FakeAuthRepository(session: UserSession) : AuthRepository {
        private val sessionFlow = MutableStateFlow(session)
        override fun observeSession(): Flow<UserSession> = sessionFlow
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = null
    }

    private class FakeParticipantRepository : ParticipantRepository {
        var createdParticipant: Participant? = null

        override fun observeParticipants(includeInactive: Boolean): Flow<AppResult<ParticipantListSnapshot>> =
            flowOf(AppResult.Success(ParticipantListSnapshot(emptyList())))

        override fun observeParticipant(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>> =
            flowOf(AppResult.Success(ParticipantDetailSnapshot(null)))

        override suspend fun getParticipant(participantId: String): AppResult<Participant?> =
            AppResult.Success(null)

        override suspend fun findParticipantByPhone(phoneNormalized: String): AppResult<Participant?> =
            AppResult.Success(null)

        override suspend fun createParticipant(participant: Participant): AppResult<Participant> {
            createdParticipant = participant
            return AppResult.Success(participant.copy(id = "generated-id"))
        }

        override suspend fun updateParticipant(actorId: String, participant: Participant): AppResult<Participant> =
            AppResult.Success(participant)

        override suspend fun archiveParticipant(actorId: String, participantId: String): AppResult<Unit> =
            AppResult.Success(Unit)
    }

    @Test
    fun `unauthenticated user returns Failure`() = runBlocking {
        val authRepo = FakeAuthRepository(UserSession.Unauthenticated)
        val participantRepo = FakeParticipantRepository()
        val useCase = CreateParticipantUseCase(participantRepo, authRepo)

        val result = useCase("Budi", "08123456789")
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `regular active user can create participant`() = runBlocking {
        val profile = UserProfile(
            id = "user-1",
            displayName = "User Biasa",
            email = "user@test.com",
            role = UserRole.USER,
            active = true,
            invitationStatus = InvitationStatus.ACTIVE,
            createdBy = "admin-1",
            createdAt = Instant.now(),
            updatedBy = "admin-1",
            updatedAt = Instant.now(),
        )
        val authRepo = FakeAuthRepository(UserSession.Authenticated(profile))
        val participantRepo = FakeParticipantRepository()
        val useCase = CreateParticipantUseCase(participantRepo, authRepo)

        val result = useCase(
            name = "Siti Rahma",
            phoneRaw = "0812-3456-7890",
            email = "siti@example.com",
            notes = "Peserta baru",
        )

        assertTrue(result is AppResult.Success)
        val created = participantRepo.createdParticipant
        assertEquals("Siti Rahma", created?.name)
        assertEquals("6281234567890", created?.phoneNormalized)
        assertEquals("0812-3456-7890", created?.phoneDisplay)
        assertEquals("user-1", created?.createdBy)
    }

    @Test
    fun `invalid phone number returns Failure`() = runBlocking {
        val profile = UserProfile(
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
        val authRepo = FakeAuthRepository(UserSession.Authenticated(profile))
        val participantRepo = FakeParticipantRepository()
        val useCase = CreateParticipantUseCase(participantRepo, authRepo)

        val result = useCase("Budi", "abc")
        assertTrue(result is AppResult.Failure)
    }
}
