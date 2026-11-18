package com.harunmaul.epelatihan.feature.participant.list

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
import com.harunmaul.epelatihan.domain.usecase.ObserveParticipantsUseCase
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
class ParticipantListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleParticipant1 = Participant(
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

    private val sampleParticipant2 = Participant(
        id = "p-2",
        name = "Budi Hartono",
        nameSort = "budi hartono",
        phoneDisplay = "0813-3333-4444",
        phoneNormalized = "6281333334444",
        email = "budi@example.com",
        notes = "Catatan 2",
        active = false,
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

    private class FakeParticipantRepository(
        private val participants: List<Participant>,
    ) : ParticipantRepository {
        override fun observeParticipants(includeInactive: Boolean): Flow<AppResult<ParticipantListSnapshot>> {
            val list = if (includeInactive) participants else participants.filter { it.active }
            return flowOf(AppResult.Success(ParticipantListSnapshot(list, isFromCache = false)))
        }

        override fun observeParticipant(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>> =
            flowOf(AppResult.Success(ParticipantDetailSnapshot(participants.find { it.id == participantId })))

        override suspend fun getParticipant(participantId: String): AppResult<Participant?> =
            AppResult.Success(participants.find { it.id == participantId })

        override suspend fun findParticipantByPhone(phoneNormalized: String): AppResult<Participant?> =
            AppResult.Success(participants.find { it.phoneNormalized == phoneNormalized })

        override suspend fun createParticipant(participant: Participant): AppResult<Participant> =
            AppResult.Success(participant)

        override suspend fun updateParticipant(actorId: String, participant: Participant): AppResult<Participant> =
            AppResult.Success(participant)

        override suspend fun archiveParticipant(actorId: String, participantId: String): AppResult<Unit> =
            AppResult.Success(Unit)
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
    fun `admin user sees active participants by default and can toggle inactive`() = runTest {
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
        val participantRepo = FakeParticipantRepository(listOf(sampleParticipant1, sampleParticipant2))
        val observeParticipants = ObserveParticipantsUseCase(participantRepo)
        val observeSession = ObserveSessionUseCase(authRepo)

        val viewModel = ParticipantListViewModel(observeParticipants, observeSession)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isAdmin)
        assertEquals(1, state.participants.size)
        assertEquals("p-1", state.participants[0].id)

        // Toggle inactive
        viewModel.onIncludeInactiveToggled(true)
        advanceUntilIdle()

        val stateWithInactive = viewModel.uiState.value
        assertEquals(2, stateWithInactive.participants.size)
    }

    @Test
    fun `search query filters participants by name or phone`() = runTest {
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
        val participantRepo = FakeParticipantRepository(listOf(sampleParticipant1, sampleParticipant2))
        val observeParticipants = ObserveParticipantsUseCase(participantRepo)
        val observeSession = ObserveSessionUseCase(authRepo)

        val viewModel = ParticipantListViewModel(observeParticipants, observeSession)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("1111")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.participants.size)
        assertEquals("Ahmad Dani", state.participants[0].name)
    }
}
