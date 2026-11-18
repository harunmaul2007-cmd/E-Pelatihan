package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.RegistrationDetailSnapshot
import com.harunmaul.epelatihan.domain.model.RegistrationListSnapshot
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePaymentStatusUseCaseTest {

    private val sampleRegistration = Registration(
        id = "reg-1",
        trainingId = "t-1",
        participantId = "p-1",
        pricingType = PricingType.NORMAL,
        priceAmount = 500000,
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

    private val inactiveAdminProfile = adminProfile.copy(active = false)

    private class FakeAuthRepo(private val profile: UserProfile?) : AuthRepository {
        override fun observeSession(): Flow<UserSession> = flowOf(
            profile?.let { UserSession.Authenticated(it) } ?: UserSession.Unauthenticated,
        )
        override suspend fun login(email: String, password: String): AppResult<UserProfile> = AppResult.Success(profile!!)
        override suspend fun sendPasswordReset(email: String): AppResult<Unit> = AppResult.Success(Unit)
        override suspend fun getCurrentUserProfile(): UserProfile? = profile
        override suspend fun logout() {}
    }

    private class FakeRegistrationRepo(
        private var registration: Registration,
    ) : RegistrationRepository {
        var lastActorId: String? = null
        var lastNewStatus: PaymentStatus? = null

        override fun observeRegistrationsByTraining(trainingId: String): Flow<AppResult<RegistrationListSnapshot>> =
            flowOf(AppResult.Success(RegistrationListSnapshot(listOf(registration), false)))

        override fun observeRegistration(registrationId: String): Flow<AppResult<RegistrationDetailSnapshot>> =
            flowOf(AppResult.Success(RegistrationDetailSnapshot(registration, false)))

        override suspend fun getRegistration(registrationId: String): AppResult<Registration?> =
            AppResult.Success(registration)

        override suspend fun getRegistrationsByTraining(trainingId: String): AppResult<List<Registration>> =
            AppResult.Success(listOf(registration))

        override suspend fun createRegistration(registration: Registration): AppResult<Registration> =
            AppResult.Success(registration)

        override suspend fun updateRegistrationPrice(
            actorId: String,
            registrationId: String,
            newPricingType: PricingType,
            newPriceAmount: Long,
        ): AppResult<Registration> {
            registration = registration.copy(pricingType = newPricingType, priceAmount = newPriceAmount)
            return AppResult.Success(registration)
        }

        override suspend fun updatePaymentStatus(
            actorId: String,
            registrationId: String,
            newStatus: PaymentStatus,
        ): AppResult<Registration> {
            lastActorId = actorId
            lastNewStatus = newStatus
            registration = registration.copy(
                paymentStatus = newStatus,
                paidAt = if (newStatus == PaymentStatus.LUNAS) Instant.now() else null,
                paidBy = if (newStatus == PaymentStatus.LUNAS) actorId else null,
                updatedBy = actorId,
            )
            return AppResult.Success(registration)
        }
    }

    @Test
    fun `admin marks payment as LUNAS succeeds and sets paid metadata`() = runBlocking {
        val regRepo = FakeRegistrationRepo(sampleRegistration)
        val authRepo = FakeAuthRepo(adminProfile)
        val useCase = UpdatePaymentStatusUseCase(regRepo, authRepo)

        val result = useCase(registrationId = "reg-1", newStatus = PaymentStatus.LUNAS)

        assertTrue(result is AppResult.Success)
        val updated = (result as AppResult.Success).value
        assertEquals(PaymentStatus.LUNAS, updated.paymentStatus)
        assertEquals("admin-1", regRepo.lastActorId)
        assertEquals(PaymentStatus.LUNAS, regRepo.lastNewStatus)
        assertEquals("admin-1", updated.paidBy)
        assertTrue(updated.paidAt != null)
    }

    @Test
    fun `admin reverts payment to BELUM_BAYAR succeeds and clears paid metadata`() = runBlocking {
        val paidRegistration = sampleRegistration.copy(
            paymentStatus = PaymentStatus.LUNAS,
            paidAt = Instant.now(),
            paidBy = "admin-1",
        )
        val regRepo = FakeRegistrationRepo(paidRegistration)
        val authRepo = FakeAuthRepo(adminProfile)
        val useCase = UpdatePaymentStatusUseCase(regRepo, authRepo)

        val result = useCase(registrationId = "reg-1", newStatus = PaymentStatus.BELUM_BAYAR)

        assertTrue(result is AppResult.Success)
        val updated = (result as AppResult.Success).value
        assertEquals(PaymentStatus.BELUM_BAYAR, updated.paymentStatus)
        assertEquals("admin-1", regRepo.lastActorId)
        assertEquals(PaymentStatus.BELUM_BAYAR, regRepo.lastNewStatus)
        assertEquals(null, updated.paidBy)
        assertEquals(null, updated.paidAt)
    }

    @Test
    fun `regular user fails to update payment status with AuthorizationException`() = runBlocking {
        val regRepo = FakeRegistrationRepo(sampleRegistration)
        val authRepo = FakeAuthRepo(userProfile)
        val useCase = UpdatePaymentStatusUseCase(regRepo, authRepo)

        val result = useCase(registrationId = "reg-1", newStatus = PaymentStatus.LUNAS)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
    }

    @Test
    fun `inactive admin fails to update payment status with InactiveAccountException`() = runBlocking {
        val regRepo = FakeRegistrationRepo(sampleRegistration)
        val authRepo = FakeAuthRepo(inactiveAdminProfile)
        val useCase = UpdatePaymentStatusUseCase(regRepo, authRepo)

        val result = useCase(registrationId = "reg-1", newStatus = PaymentStatus.LUNAS)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is InactiveAccountException)
    }

    @Test
    fun `unauthenticated user fails with AuthorizationException`() = runBlocking {
        val regRepo = FakeRegistrationRepo(sampleRegistration)
        val authRepo = FakeAuthRepo(null)
        val useCase = UpdatePaymentStatusUseCase(regRepo, authRepo)

        val result = useCase(registrationId = "reg-1", newStatus = PaymentStatus.LUNAS)

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).cause is AuthorizationException)
    }
}
