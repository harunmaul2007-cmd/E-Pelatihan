package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.ParticipantDetailSnapshot
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveParticipantDetailUseCase @Inject constructor(
    private val participantRepository: ParticipantRepository,
) {
    operator fun invoke(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>> =
        participantRepository.observeParticipant(participantId)
}
