package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.DuplicateRegistrationException
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.ParticipantDetailSnapshot
import com.harunmaul.epelatihan.domain.model.ParticipantListSnapshot
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.RegistrationDetailSnapshot
import com.harunmaul.epelatihan.domain.model.RegistrationListSnapshot
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterParticipantUseCaseTest {

    private val sampleTrainingNormalOnly = Training(
        id = "t-1",
        name = "Pelatihan Android",
        description = "Deskripsi",
        startAt = Instant.now(),
        endAt = Instant.now().plusSeconds(3600),
        timeZoneId = "Asia/Jakarta",
        speaker = "Pemateri",
        meetingMethod = MeetingMethod.ONLINE,
        zoomUrl = "https://zoom.us/j/123",
        location = null,
        recordingUrl = null,
        materialUrl = null,
        pricingScheme = PricingScheme.NORMAL_ONLY,
        normalPrice = 300000,
        earlyBirdPrice = null,
        lifecycle = TrainingLifecycle.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val sampleTrainingEarlyBird = Training(
        id = "t-2",
        name = "Pelatihan Kotlin Multiplatform",
        description = "Deskripsi",
        startAt = Instant.now(),
        endAt = Instant.now().plusSeconds(3600),
        timeZoneId = "Asia/Jakarta",
        speaker = "Pemateri",
        meetingMethod = MeetingMethod.ONLINE,
        zoomUrl = "https://zoom.us/j/123",
        location = null,
        recordingUrl = null,
        materialUrl = null,
        pricingScheme = PricingScheme.EARLY_BIRD_AND_NORMAL,
        normalPrice = 500000,
        earlyBirdPrice = 400000,
        lifecycle = TrainingLifecycle.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val userProfile = UserProfile(
        id = "user-1",
        displayName = "User",
        email = "user@example.com",
        role = UserRole.USER,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private class FakeAuthRepo(private val profile: UserProfile?) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(
            profile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated,
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = error("Not needed")
        override suspend fun logout() {}
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = profile
    }

    private class FakeRegistrationRepo : RegistrationRepository {
        var createdRegistration: Registration? = null
        var shouldThrowDuplicate = false

        override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> =
            flowOf(AppResult.Success(RegistrationListSnapshot(emptyList())))

        override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> =
            flowOf(AppResult.Success(RegistrationDetailSnapshot(null)))

        override suspend fun getRegistration(registrationId: String): AppResult<Registration?> =
            AppResult.Success(null)

        override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> =
            AppResult.Success(emptyList())

        override suspend fun createRegistration(registration: Registration): AppResult<Registration> {
            if (shouldThrowDuplicate) {
                return AppResult.Failure(DuplicateRegistrationException())
            }
            createdRegistration = registration
            return AppResult.Success(registration)
        }

        override suspend fun updateRegistrationPrice(
            actorId: String,
            registrationId: String,
            newPricingType: PricingType,
            newPriceAmount: Long,
        ): AppResult<Registration> = error("Not needed")

        override suspend fun updatePaymentStatus(
            actorId: String,
            registrationId: String,
            newStatus: PaymentStatus,
        ): AppResult<Registration> = error("Not needed")
    }

    private class FakeParticipantRepo : ParticipantRepository {
        override fun observeParticipants(includeInactive: Boolean): Flow<AppResult<ParticipantListSnapshot>> =
            flowOf(AppResult.Success(ParticipantListSnapshot(emptyList())))

        override fun observeParticipant(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>> =
            flowOf(AppResult.Success(ParticipantDetailSnapshot(null)))

        override suspend fun getParticipant(participantId: String): AppResult<Participant?> =
            AppResult.Success(null)

        override suspend fun findParticipantByPhone(phoneNormalized: String): AppResult<Participant?> =
            AppResult.Success(null)

        override suspend fun createParticipant(participant: Participant): AppResult<Participant> =
            AppResult.Success(participant.copy(id = "new-part-id"))

        override suspend fun updateParticipant(actorId: String, participant: Participant): AppResult<Participant> =
            AppResult.Success(participant)

        override suspend fun archiveParticipant(actorId: String, participantId: String): AppResult<Unit> =
            AppResult.Success(Unit)
    }

    @Test
    fun `normal only training automatically selects normal price`() = runBlocking {
        val authRepo = FakeAuthRepo(userProfile)
        val regRepo = FakeRegistrationRepo()
        val partRepo = FakeParticipantRepo()
        val createPartUseCase = CreateParticipantUseCase(partRepo, authRepo)
        val useCase = RegisterParticipantUseCase(regRepo, createPartUseCase, authRepo)

        val result = useCase(
            training = sampleTrainingNormalOnly,
            participantId = "p-100",
            selectedPricingType = null,
        )

        assertTrue(result is AppResult.Success)
        val reg = regRepo.createdRegistration
        assertEquals(PricingType.NORMAL, reg?.pricingType)
        assertEquals(300000L, reg?.priceAmount)
        assertEquals(PaymentStatus.BELUM_BAYAR, reg?.paymentStatus)
        assertEquals("user-1", reg?.createdBy)
    }

    @Test
    fun `early bird training requires explicit pricing choice and saves chosen amount`() = runBlocking {
        val authRepo = FakeAuthRepo(userProfile)
        val regRepo = FakeRegistrationRepo()
        val partRepo = FakeParticipantRepo()
        val createPartUseCase = CreateParticipantUseCase(partRepo, authRepo)
        val useCase = RegisterParticipantUseCase(regRepo, createPartUseCase, authRepo)

        // Without selecting pricing type -> fails
        val failResult = useCase(
            training = sampleTrainingEarlyBird,
            participantId = "p-100",
            selectedPricingType = null,
        )
        assertTrue(failResult is AppResult.Failure)

        // Selecting Early Bird -> succeeds with 400000
        val successResult = useCase(
            training = sampleTrainingEarlyBird,
            participantId = "p-100",
            selectedPricingType = PricingType.EARLY_BIRD,
        )
        assertTrue(successResult is AppResult.Success)
        val reg = regRepo.createdRegistration
        assertEquals(PricingType.EARLY_BIRD, reg?.pricingType)
        assertEquals(400000L, reg?.priceAmount)
    }

    @Test
    fun `duplicate registration returns failure`() = runBlocking {
        val authRepo = FakeAuthRepo(userProfile)
        val regRepo = FakeRegistrationRepo()
        regRepo.shouldThrowDuplicate = true
        val partRepo = FakeParticipantRepo()
        val createPartUseCase = CreateParticipantUseCase(partRepo, authRepo)
        val useCase = RegisterParticipantUseCase(regRepo, createPartUseCase, authRepo)

        val result = useCase(
            training = sampleTrainingNormalOnly,
            participantId = "p-100",
            selectedPricingType = PricingType.NORMAL,
        )
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is DuplicateRegistrationException)
    }
}
