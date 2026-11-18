package com.harunmaul.epelatihan.domain.repository

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.ParticipantDetailSnapshot
import com.harunmaul.epelatihan.domain.model.ParticipantListSnapshot
import kotlinx.coroutines.flow.Flow

interface ParticipantRepository {
    fun observeParticipants(includeInactive: Boolean = false): Flow<AppResult<ParticipantListSnapshot>>
    fun observeParticipant(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>>
    suspend fun getParticipant(participantId: String): AppResult<Participant?>
    suspend fun findParticipantByPhone(phoneNormalized: String): AppResult<Participant?>
    suspend fun createParticipant(participant: Participant): AppResult<Participant>
    suspend fun updateParticipant(actorId: String, participant: Participant): AppResult<Participant>
    suspend fun archiveParticipant(actorId: String, participantId: String): AppResult<Unit>
}
