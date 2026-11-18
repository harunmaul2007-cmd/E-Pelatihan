package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.core.common.ValidationException
import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import com.harunmaul.epelatihan.domain.repository.AuditLogRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.validation.AppSettingsValidator
import com.harunmaul.epelatihan.domain.validation.ValidationResult
import javax.inject.Inject
import kotlinx.coroutines.flow.first

open class UpdateCommunityNameUseCase @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val auditLogRepository: AuditLogRepository,
    private val authRepository: AuthRepository,
) {
    open suspend operator fun invoke(communityName: String): AppResult<AppSettings> {
        val session = authRepository.observeSession().first()
        val authenticated = session as? UserSession.Authenticated
            ?: return AppResult.Failure(AuthorizationException("Pengguna belum login."))

        if (!authenticated.profile.active) {
            return AppResult.Failure(InactiveAccountException())
        }

        if (authenticated.profile.role != UserRole.ADMIN) {
            return AppResult.Failure(AuthorizationException("Hanya Admin yang dapat mengubah nama komunitas."))
        }

        val validation = AppSettingsValidator.validateCommunityName(communityName)
        if (validation is ValidationResult.Invalid) {
            val message = validation.issues.first().message
            return AppResult.Failure(ValidationException(message))
        }

        val normalized = AppSettingsValidator.normalizeCommunityName(communityName)
        val currentSettings = appSettingsRepository.getAppSettings()
        val oldName = currentSettings.communityName

        val result = appSettingsRepository.updateCommunityName(
            actorId = authenticated.profile.id,
            communityName = normalized,
        )

        if (result is AppResult.Success) {
            auditLogRepository.recordLog(
                actorId = authenticated.profile.id,
                action = "UPDATE_COMMUNITY_NAME",
                entityType = "APP_SETTINGS",
                entityId = "general",
                before = mapOf("communityName" to oldName),
                after = mapOf("communityName" to normalized),
            )
        }

        return result
    }
}
