package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class UpdatePaymentStatusUseCase @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        registrationId: String,
        newStatus: PaymentStatus,
    ): AppResult<Registration> {
        val session = authRepository.observeSession().first()
        val authenticated = session as? UserSession.Authenticated
            ?: return AppResult.Failure(AuthorizationException("Pengguna belum login."))

        if (!authenticated.profile.active) {
            return AppResult.Failure(InactiveAccountException())
        }

        if (authenticated.profile.role != UserRole.ADMIN) {
            return AppResult.Failure(AuthorizationException("Hanya Admin yang dapat mengubah status pembayaran."))
        }

        return registrationRepository.updatePaymentStatus(
            actorId = authenticated.profile.id,
            registrationId = registrationId,
            newStatus = newStatus,
        )
    }
}
