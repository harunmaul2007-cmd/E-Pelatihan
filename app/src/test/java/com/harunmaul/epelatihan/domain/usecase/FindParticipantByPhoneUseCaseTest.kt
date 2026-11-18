package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.ParticipantDetailSnapshot
import com.harunmaul.epelatihan.domain.model.ParticipantListSnapshot
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FindParticipantByPhoneUseCaseTest {

    private class FakeParticipantRepository : ParticipantRepository {
        var searchedPhone: String? = null
        var participantToReturn: Participant? = null

        override fun observeParticipants(includeInactive: Boolean): Flow<AppResult<ParticipantListSnapshot>> =
            flowOf(AppResult.Success(ParticipantListSnapshot(emptyList())))

        override fun observeParticipant(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>> =
            flowOf(AppResult.Success(ParticipantDetailSnapshot(null)))

        override suspend fun getParticipant(participantId: String): AppResult<Participant?> =
            AppResult.Success(null)

        override suspend fun findParticipantByPhone(phoneNormalized: String): AppResult<Participant?> {
            searchedPhone = phoneNormalized
            return AppResult.Success(participantToReturn)
        }

        override suspend fun createParticipant(participant: Participant): AppResult<Participant> =
            AppResult.Success(participant)

        override suspend fun updateParticipant(actorId: String, participant: Participant): AppResult<Participant> =
            AppResult.Success(participant)

        override suspend fun archiveParticipant(actorId: String, participantId: String): AppResult<Unit> =
            AppResult.Success(Unit)
    }

    @Test
    fun `invalid raw phone returns Failure`() = runBlocking {
        val repo = FakeParticipantRepository()
        val useCase = FindParticipantByPhoneUseCase(repo)

        val result = useCase("invalid-phone")
        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `valid raw phone normalizes and returns existing participant`() = runBlocking {
        val repo = FakeParticipantRepository()
        val existing = Participant(
            id = "p-1",
            name = "Andi",
            nameSort = "andi",
            phoneDisplay = "0812-3456-789",
            phoneNormalized = "628123456789",
            email = null,
            notes = null,
            active = true,
            createdBy = "uid-1",
            createdAt = null,
            updatedBy = "uid-1",
            updatedAt = null,
        )
        repo.participantToReturn = existing
        val useCase = FindParticipantByPhoneUseCase(repo)

        val result = useCase("0812-3456-789")
        assertTrue(result is AppResult.Success)
        assertEquals("628123456789", repo.searchedPhone)
        assertEquals(existing, (result as AppResult.Success).value)
    }
}
