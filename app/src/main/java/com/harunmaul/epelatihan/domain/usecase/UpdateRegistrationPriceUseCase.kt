package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.validation.PriceResolution
import com.harunmaul.epelatihan.domain.validation.RegistrationPricingPolicy
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class UpdateRegistrationPriceUseCase @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val trainingRepository: TrainingRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        registrationId: String,
        newPricingType: PricingType,
    ): AppResult<Registration> {
        val session = authRepository.observeSession().first()
        val authenticated = session as? UserSession.Authenticated
            ?: return AppResult.Failure(AuthorizationException("Pengguna belum login."))

        if (!authenticated.profile.active) {
            return AppResult.Failure(InactiveAccountException())
        }

        if (authenticated.profile.role != UserRole.ADMIN) {
            return AppResult.Failure(AuthorizationException("Hanya Admin yang dapat mengubah jenis harga pendaftaran."))
        }

        val regResult = registrationRepository.getRegistration(registrationId)
        val registration = when (regResult) {
            is AppResult.Success -> regResult.value
                ?: return AppResult.Failure(NoSuchElementException("Pendaftaran tidak ditemukan."))
            is AppResult.Failure -> return AppResult.Failure(regResult.cause)
        }

        val trainingResult = trainingRepository.getTraining(registration.trainingId)
        val training = when (trainingResult) {
            is AppResult.Success -> trainingResult.value
                ?: return AppResult.Failure(NoSuchElementException("Pelatihan tidak ditemukan."))
            is AppResult.Failure -> return AppResult.Failure(trainingResult.cause)
        }

        val priceResolution = RegistrationPricingPolicy.resolve(training, newPricingType)
        val resolvedPrice = when (priceResolution) {
            is PriceResolution.Rejected -> return AppResult.Failure(ValidationException(priceResolution.issue.message))
            is PriceResolution.Resolved -> priceResolution
        }

        return registrationRepository.updateRegistrationPrice(
            actorId = authenticated.profile.id,
            registrationId = registrationId,
            newPricingType = resolvedPrice.pricingType,
            newPriceAmount = resolvedPrice.amount,
        )
    }
}
