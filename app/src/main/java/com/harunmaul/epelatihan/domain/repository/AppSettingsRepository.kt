package com.harunmaul.epelatihan.domain.repository

import com.harunmaul.epelatihan.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeAppSettings(): Flow<AppSettings>
    suspend fun getAppSettings(): AppSettings
}
