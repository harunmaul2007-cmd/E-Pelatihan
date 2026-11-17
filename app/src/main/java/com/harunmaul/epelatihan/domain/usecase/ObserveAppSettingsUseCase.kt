package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveAppSettingsUseCase @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = appSettingsRepository.observeAppSettings()
}
