package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ArchiveParticipantUseCase @Inject constructor(
    private val participantRepository: ParticipantRepository,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(participantId: String): AppResult<Unit> {
        val session = authRepository.observeSession().first()
        val authenticated = session as? UserSession.Authenticated
            ?: return AppResult.Failure(AuthorizationException("Pengguna belum login."))

        if (!authenticated.profile.active) {
            return AppResult.Failure(InactiveAccountException())
        }

        if (authenticated.profile.role != UserRole.ADMIN) {
            return AppResult.Failure(AuthorizationException("Hanya Admin yang dapat mengarsipkan data peserta."))
        }

        return participantRepository.archiveParticipant(authenticated.profile.id, participantId)
    }
}
