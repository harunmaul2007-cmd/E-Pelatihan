package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.RegistrationDetailSnapshot
import com.harunmaul.epelatihan.domain.model.RegistrationListSnapshot
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingDetailSnapshot
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.TrainingListSnapshot
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRegistrationPriceUseCaseTest {

    private val sampleTraining = Training(
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
        pricingScheme = PricingScheme.EARLY_BIRD_AND_NORMAL,
        normalPrice = 500000,
        earlyBirdPrice = 400000,
        lifecycle = TrainingLifecycle.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val sampleRegistration = Registration(
        id = "reg-1",
        trainingId = "t-1",
        participantId = "p-1",
        pricingType = PricingType.EARLY_BIRD,
        priceAmount = 400000,
        paymentStatus = PaymentStatus.BELUM_BAYAR,
        paidAt = null,
        paidBy = null,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val adminProfile = UserProfile(
        id = "admin-1",
        displayName = "Admin",
        email = "admin@example.com",
        role = UserRole.ADMIN,
        active = true,
        invitationStatus = InvitationStatus.ACTIVE,
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

    private class FakeRegistrationRepo(var reg: Registration) : RegistrationRepository {
        var updatedPricingType: PricingType? = null
        var updatedPriceAmount: Long? = null

        override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> =
            flowOf(AppResult.Success(RegistrationListSnapshot(listOf(reg))))

        override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> =
            flowOf(AppResult.Success(RegistrationDetailSnapshot(reg)))

        override suspend fun getRegistration(registrationId: String): AppResult<Registration?> =
            AppResult.Success(reg)

        override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> =
            AppResult.Success(listOf(reg))

        override suspend fun createRegistration(registration: Registration): AppResult<Registration> =
            AppResult.Success(registration)

        override suspend fun updateRegistrationPrice(
            actorId: String,
            registrationId: String,
            newPricingType: PricingType,
            newPriceAmount: Long,
        ): AppResult<Registration> {
            updatedPricingType = newPricingType
            updatedPriceAmount = newPriceAmount
            val updated = reg.copy(pricingType = newPricingType, priceAmount = newPriceAmount)
            reg = updated
            return AppResult.Success(updated)
        }

        override suspend fun updatePaymentStatus(
            actorId: String,
            registrationId: String,
            newStatus: PaymentStatus,
        ): AppResult<Registration> = error("Not needed")
    }

    private class FakeTrainingRepo(private val training: Training) : TrainingRepository {
        override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> =
            flowOf(AppResult.Success(TrainingListSnapshot(emptyList())))

        override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> =
            flowOf(AppResult.Success(TrainingDetailSnapshot(training)))

        override suspend fun getTraining(trainingId: String): AppResult<Training?> =
            AppResult.Success(training)

        override suspend fun createTraining(training: Training): AppResult<Training> = error("Not needed")
        override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> = error("Not needed")
        override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> = error("Not needed")
    }

    @Test
    fun `regular user cannot update registration price`() = runBlocking {
        val authRepo = FakeAuthRepo(userProfile)
        val regRepo = FakeRegistrationRepo(sampleRegistration)
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val useCase = UpdateRegistrationPriceUseCase(regRepo, trainingRepo, authRepo)

        val result = useCase("reg-1", PricingType.NORMAL)
        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
    }

    @Test
    fun `admin can update price from early bird to normal`() = runBlocking {
        val authRepo = FakeAuthRepo(adminProfile)
        val regRepo = FakeRegistrationRepo(sampleRegistration)
        val trainingRepo = FakeTrainingRepo(sampleTraining)
        val useCase = UpdateRegistrationPriceUseCase(regRepo, trainingRepo, authRepo)

        val result = useCase("reg-1", PricingType.NORMAL)
        assertTrue(result is AppResult.Success)
        assertEquals(PricingType.NORMAL, regRepo.updatedPricingType)
        assertEquals(500000L, regRepo.updatedPriceAmount)
    }
}
