package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.RegistrationWithParticipant
import com.harunmaul.epelatihan.domain.model.TrainingRegistrationsSnapshot
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveTrainingRegistrationsUseCase @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val participantRepository: ParticipantRepository,
) {
    operator fun invoke(trainingId: String): Flow<AppResult<TrainingRegistrationsSnapshot>> {
        return combine(
            registrationRepository.observeRegistrationsByTraining(trainingId),
            participantRepository.observeParticipants(includeInactive = true),
        ) { regResult, partResult ->
            when {
                regResult is AppResult.Failure -> AppResult.Failure(regResult.cause)
                regResult is AppResult.Success -> {
                    val participantMap = if (partResult is AppResult.Success) {
                        partResult.value.participants.associateBy { it.id }
                    } else {
                        emptyMap()
                    }
                    val items = regResult.value.registrations.map { reg ->
                        RegistrationWithParticipant(
                            registration = reg,
                            participant = participantMap[reg.participantId],
                        )
                    }
                    val isFromCache = regResult.value.isFromCache ||
                        (partResult is AppResult.Success && partResult.value.isFromCache)
                    AppResult.Success(TrainingRegistrationsSnapshot(items, isFromCache))
                }
                else -> AppResult.Success(TrainingRegistrationsSnapshot(emptyList(), false))
            }
        }
    }
}
