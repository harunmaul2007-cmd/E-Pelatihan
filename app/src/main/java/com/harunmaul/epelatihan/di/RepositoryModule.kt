package com.harunmaul.epelatihan.di

import com.harunmaul.epelatihan.data.repository.DefaultAppInfoRepository
import com.harunmaul.epelatihan.domain.repository.AppInfoRepository
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
}
