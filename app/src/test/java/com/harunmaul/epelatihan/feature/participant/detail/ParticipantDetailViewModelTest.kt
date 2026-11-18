package com.harunmaul.epelatihan.feature.participant.detail

import androidx.lifecycle.SavedStateHandle
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
import com.harunmaul.epelatihan.domain.usecase.ArchiveParticipantUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveParticipantDetailUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParticipantDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleParticipant = Participant(
        id = "p-1",
        name = "Ahmad Dani",
        nameSort = "ahmad dani",
        phoneDisplay = "0812-1111-2222",
        phoneNormalized = "6281211112222",
        email = "ahmad@example.com",
        notes = "Catatan 1",
        active = true,
        createdBy = "admin-1",
        createdAt = null,
        updatedBy = "admin-1",
        updatedAt = null,
    )

    private class FakeAuthRepository(private val session: UserSession) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(session)
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = null
    }

    private class FakeParticipantRepository : ParticipantRepository {
        var archivedId: String? = null

        override fun observeParticipants(includeInactive: Boolean): Flow<AppResult<ParticipantListSnapshot>> =
            flowOf(AppResult.Success(ParticipantListSnapshot(emptyList())))

        override fun observeParticipant(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>> =
            flowOf(AppResult.Success(ParticipantDetailSnapshot(
                Participant(
                    id = participantId,
                    name = "Ahmad Dani",
                    nameSort = "ahmad dani",
                    phoneDisplay = "0812-1111-2222",
                    phoneNormalized = "6281211112222",
                    email = "ahmad@example.com",
                    notes = "Catatan 1",
                    active = true,
                    createdBy = "admin-1",
                    createdAt = null,
                    updatedBy = "admin-1",
                    updatedAt = null,
                ),
            )))

        override suspend fun getParticipant(participantId: String): AppResult<Participant?> =
            AppResult.Success(null)

        override suspend fun findParticipantByPhone(phoneNormalized: String): AppResult<Participant?> =
            AppResult.Success(null)

        override suspend fun createParticipant(participant: Participant): AppResult<Participant> =
            AppResult.Success(participant)

        override suspend fun updateParticipant(actorId: String, participant: Participant): AppResult<Participant> =
            AppResult.Success(participant)

        override suspend fun archiveParticipant(actorId: String, participantId: String): AppResult<Unit> {
            archivedId = participantId
            return AppResult.Success(Unit)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads participant detail and allows admin to archive`() = runTest {
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
        val observeDetail = ObserveParticipantDetailUseCase(participantRepo)
        val archiveUseCase = ArchiveParticipantUseCase(participantRepo, authRepo)
        val observeSession = ObserveSessionUseCase(authRepo)

        val savedStateHandle = SavedStateHandle(mapOf("participantId" to "p-1"))
        val viewModel = ParticipantDetailViewModel(
            savedStateHandle = savedStateHandle,
            observeParticipantDetailUseCase = observeDetail,
            archiveParticipantUseCase = archiveUseCase,
            observeSessionUseCase = observeSession,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isAdmin)
        assertEquals("p-1", state.participant?.id)
        assertEquals("Ahmad Dani", state.participant?.name)

        viewModel.archiveParticipant()
        advanceUntilIdle()

        val afterArchive = viewModel.uiState.value
        assertTrue(afterArchive.isArchivedSuccess)
        assertEquals("p-1", participantRepo.archivedId)
    }
}
