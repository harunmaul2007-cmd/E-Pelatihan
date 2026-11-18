package com.harunmaul.epelatihan.feature.participant.form

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
import com.harunmaul.epelatihan.domain.usecase.CreateParticipantUseCase
import com.harunmaul.epelatihan.domain.usecase.FindParticipantByPhoneUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveParticipantDetailUseCase
import com.harunmaul.epelatihan.domain.usecase.ObserveSessionUseCase
import com.harunmaul.epelatihan.domain.usecase.UpdateParticipantUseCase
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
class ParticipantFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeAuthRepository(private val session: UserSession) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(session)
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = null
    }

    private class FakeParticipantRepository : ParticipantRepository {
        var createdParticipant: Participant? = null
        var existingByPhone: Participant? = null

        override fun observeParticipants(includeInactive: Boolean): Flow<AppResult<ParticipantListSnapshot>> =
            flowOf(AppResult.Success(ParticipantListSnapshot(emptyList())))

        override fun observeParticipant(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>> =
            flowOf(AppResult.Success(ParticipantDetailSnapshot(
                Participant(
                    id = participantId,
                    name = "Existing Participant",
                    nameSort = "existing participant",
                    phoneDisplay = "08123456789",
                    phoneNormalized = "628123456789",
                    email = "existing@test.com",
                    notes = "Catatan lama",
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
            AppResult.Success(existingByPhone)

        override suspend fun createParticipant(participant: Participant): AppResult<Participant> {
            createdParticipant = participant
            return AppResult.Success(participant.copy(id = "p-new"))
        }

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
    fun `selecting contact populates name and phone, and normalizes phone`() = runTest {
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
        val createUseCase = CreateParticipantUseCase(participantRepo, authRepo)
        val updateUseCase = UpdateParticipantUseCase(participantRepo, authRepo)
        val observeDetail = ObserveParticipantDetailUseCase(participantRepo)
        val findByPhone = FindParticipantByPhoneUseCase(participantRepo)
        val observeSession = ObserveSessionUseCase(authRepo)

        val viewModel = ParticipantFormViewModel(
            savedStateHandle = SavedStateHandle(),
            createParticipantUseCase = createUseCase,
            updateParticipantUseCase = updateUseCase,
            observeParticipantDetailUseCase = observeDetail,
            findParticipantByPhoneUseCase = findByPhone,
            observeSessionUseCase = observeSession,
        )
        advanceUntilIdle()

        viewModel.onContactSelected(name = "Dewi Lestari", phone = "0812-3456-7890")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Dewi Lestari", state.name)
        assertEquals("0812-3456-7890", state.phoneRaw)
        assertEquals("6281234567890", state.phoneNormalized)
        assertTrue(state.isFormValid)
    }

    @Test
    fun `duplicate phone triggers duplicate dialog and use existing fills data`() = runTest {
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
        participantRepo.existingByPhone = Participant(
            id = "existing-id",
            name = "Rina Wijaya",
            nameSort = "rina wijaya",
            phoneDisplay = "0811-2222-3333",
            phoneNormalized = "6281122223333",
            email = "rina@example.com",
            notes = "Alumni",
            active = true,
            createdBy = "admin-1",
            createdAt = null,
            updatedBy = "admin-1",
            updatedAt = null,
        )

        val createUseCase = CreateParticipantUseCase(participantRepo, authRepo)
        val updateUseCase = UpdateParticipantUseCase(participantRepo, authRepo)
        val observeDetail = ObserveParticipantDetailUseCase(participantRepo)
        val findByPhone = FindParticipantByPhoneUseCase(participantRepo)
        val observeSession = ObserveSessionUseCase(authRepo)

        val viewModel = ParticipantFormViewModel(
            savedStateHandle = SavedStateHandle(),
            createParticipantUseCase = createUseCase,
            updateParticipantUseCase = updateUseCase,
            observeParticipantDetailUseCase = observeDetail,
            findParticipantByPhoneUseCase = findByPhone,
            observeSessionUseCase = observeSession,
        )
        advanceUntilIdle()

        viewModel.onPhoneChanged("0811-2222-3333")
        advanceUntilIdle()

        val stateWithDialog = viewModel.uiState.value
        assertTrue(stateWithDialog.showDuplicateDialog)
        assertEquals("Rina Wijaya", stateWithDialog.duplicateParticipant?.name)

        viewModel.onUseExistingParticipant()
        advanceUntilIdle()

        val stateAfterUse = viewModel.uiState.value
        assertFalse(stateAfterUse.showDuplicateDialog)
        assertEquals("Rina Wijaya", stateAfterUse.name)
        assertEquals("0811-2222-3333", stateAfterUse.phoneRaw)
        assertEquals("rina@example.com", stateAfterUse.email)
    }

    @Test
    fun `saveParticipant succeeds and triggers isSaveSuccess`() = runTest {
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
        val createUseCase = CreateParticipantUseCase(participantRepo, authRepo)
        val updateUseCase = UpdateParticipantUseCase(participantRepo, authRepo)
        val observeDetail = ObserveParticipantDetailUseCase(participantRepo)
        val findByPhone = FindParticipantByPhoneUseCase(participantRepo)
        val observeSession = ObserveSessionUseCase(authRepo)

        val viewModel = ParticipantFormViewModel(
            savedStateHandle = SavedStateHandle(),
            createParticipantUseCase = createUseCase,
            updateParticipantUseCase = updateUseCase,
            observeParticipantDetailUseCase = observeDetail,
            findParticipantByPhoneUseCase = findByPhone,
            observeSessionUseCase = observeSession,
        )
        advanceUntilIdle()

        viewModel.onNameChanged("Joko Anwar")
        viewModel.onPhoneChanged("08123456789")
        viewModel.saveParticipant()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSaveSuccess)
        assertEquals("Joko Anwar", participantRepo.createdParticipant?.name)
    }

    @Test
    fun `non-admin user in edit mode is set to read only`() = runTest {
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
        val createUseCase = CreateParticipantUseCase(participantRepo, authRepo)
        val updateUseCase = UpdateParticipantUseCase(participantRepo, authRepo)
        val observeDetail = ObserveParticipantDetailUseCase(participantRepo)
        val findByPhone = FindParticipantByPhoneUseCase(participantRepo)
        val observeSession = ObserveSessionUseCase(authRepo)

        val savedStateHandle = SavedStateHandle(mapOf("participantId" to "p-1"))
        val viewModel = ParticipantFormViewModel(
            savedStateHandle = savedStateHandle,
            createParticipantUseCase = createUseCase,
            updateParticipantUseCase = updateUseCase,
            observeParticipantDetailUseCase = observeDetail,
            findParticipantByPhoneUseCase = findByPhone,
            observeSessionUseCase = observeSession,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isEditMode)
        assertFalse(state.isAdmin)
        assertTrue(state.isReadOnlyNonAdmin)
        assertFalse(state.isFormValid)
    }
}
