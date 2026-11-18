package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.repository.WhatsAppLauncher
import com.harunmaul.epelatihan.domain.repository.WhatsAppLogRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class OpenWhatsAppUseCase @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val participantRepository: ParticipantRepository,
    private val trainingRepository: TrainingRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val authRepository: AuthRepository,
    private val buildWhatsAppMessageUseCase: BuildWhatsAppMessageUseCase,
    private val whatsAppLauncher: WhatsAppLauncher,
    private val whatsAppLogRepository: WhatsAppLogRepository,
) {
    suspend operator fun invoke(registrationId: String): AppResult<Unit> {
        val session = authRepository.observeSession().first()
        val authenticated = session as? UserSession.Authenticated
            ?: return AppResult.Failure(AuthorizationException("Pengguna belum login."))

        if (!authenticated.profile.active) {
            return AppResult.Failure(InactiveAccountException())
        }

        val regResult = registrationRepository.getRegistration(registrationId)
        val registration = when (regResult) {
            is AppResult.Success -> regResult.value
                ?: return AppResult.Failure(NoSuchElementException("Pendaftaran tidak ditemukan."))
            is AppResult.Failure -> return AppResult.Failure(regResult.cause)
        }

        if (registration.paymentStatus != PaymentStatus.LUNAS) {
            return AppResult.Failure(IllegalStateException("Pesan WhatsApp hanya dapat dikirim ke peserta yang sudah berstatus LUNAS."))
        }

        val partResult = participantRepository.getParticipant(registration.participantId)
        val participant = when (partResult) {
            is AppResult.Success -> partResult.value
                ?: return AppResult.Failure(NoSuchElementException("Data peserta tidak ditemukan."))
            is AppResult.Failure -> return AppResult.Failure(partResult.cause)
        }

        if (participant.phoneNormalized.isBlank()) {
            return AppResult.Failure(IllegalArgumentException("Nomor WhatsApp peserta tidak valid."))
        }

        val trainingResult = trainingRepository.getTraining(registration.trainingId)
        val training = when (trainingResult) {
            is AppResult.Success -> trainingResult.value
                ?: return AppResult.Failure(NoSuchElementException("Data pelatihan tidak ditemukan."))
            is AppResult.Failure -> return AppResult.Failure(trainingResult.cause)
        }

        val settings = appSettingsRepository.getAppSettings()
        val communityName = settings.communityName

        val message = buildWhatsAppMessageUseCase(
            participant = participant,
            training = training,
            communityName = communityName,
        )

        val launchResult = whatsAppLauncher.openWhatsApp(
            phoneNormalized = participant.phoneNormalized,
            message = message,
        )

        if (launchResult is AppResult.Failure) {
            return launchResult
        }

        whatsAppLogRepository.recordOpenLog(
            actorId = authenticated.profile.id,
            registrationId = registrationId,
            recipientPhone = participant.phoneNormalized,
            messageSnapshot = message,
        )

        return AppResult.Success(Unit)
    }
}
