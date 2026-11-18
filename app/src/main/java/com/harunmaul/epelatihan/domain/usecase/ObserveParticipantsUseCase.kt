package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.ParticipantListSnapshot
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveParticipantsUseCase @Inject constructor(
    private val participantRepository: ParticipantRepository,
) {
    operator fun invoke(
        query: String = "",
        includeInactive: Boolean = false,
    ): Flow<AppResult<ParticipantListSnapshot>> = participantRepository
        .observeParticipants(includeInactive)
        .map { result ->
            if (result is AppResult.Success && query.isNotBlank()) {
                val trimmed = query.trim().lowercase()
                val filtered = result.value.participants.filter { p ->
                    p.name.lowercase().contains(trimmed) ||
                        p.phoneNormalized.contains(trimmed) ||
                        p.phoneDisplay.contains(trimmed)
                }
                AppResult.Success(result.value.copy(participants = filtered))
            } else {
                result
            }
        }
}
