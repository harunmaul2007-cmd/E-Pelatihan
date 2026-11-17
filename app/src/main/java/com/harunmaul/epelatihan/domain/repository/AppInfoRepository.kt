package com.harunmaul.epelatihan.domain.repository

import com.harunmaul.epelatihan.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppInfoRepository {
    fun observeAppInfo(): Flow<AppInfo>
}
