package com.harunmaul.epelatihan.data.repository

import com.harunmaul.epelatihan.BuildConfig
import com.harunmaul.epelatihan.domain.model.AppInfo
import com.harunmaul.epelatihan.domain.repository.AppInfoRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Singleton
class DefaultAppInfoRepository @Inject constructor() : AppInfoRepository {
    override fun observeAppInfo(): Flow<AppInfo> = flowOf(
        AppInfo(
            name = "E-Pelatihan",
            environment = BuildConfig.ENVIRONMENT,
            firebaseConfigured = BuildConfig.FIREBASE_CONFIGURED,
        ),
    )
}
