package com.harunmaul.epelatihan.di

import com.harunmaul.epelatihan.data.repository.DefaultAppInfoRepository
import com.harunmaul.epelatihan.data.repository.DefaultAppSettingsRepository
import com.harunmaul.epelatihan.data.repository.DefaultAuditLogRepository
import com.harunmaul.epelatihan.data.repository.DefaultAuthRepository
import com.harunmaul.epelatihan.data.repository.DefaultTrainingRepository
import com.harunmaul.epelatihan.data.repository.DefaultUserRepository
import com.harunmaul.epelatihan.domain.repository.AppInfoRepository
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import com.harunmaul.epelatihan.domain.repository.AuditLogRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import com.harunmaul.epelatihan.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAppInfoRepository(
        implementation: DefaultAppInfoRepository,
    ): AppInfoRepository

    @Binds
    @Singleton
    abstract fun bindAppSettingsRepository(
        implementation: DefaultAppSettingsRepository,
    ): AppSettingsRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        implementation: DefaultUserRepository,
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        implementation: DefaultAuthRepository,
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindAuditLogRepository(
        implementation: DefaultAuditLogRepository,
    ): AuditLogRepository

    @Binds
    @Singleton
    abstract fun bindTrainingRepository(
        implementation: DefaultTrainingRepository,
    ): TrainingRepository
}
