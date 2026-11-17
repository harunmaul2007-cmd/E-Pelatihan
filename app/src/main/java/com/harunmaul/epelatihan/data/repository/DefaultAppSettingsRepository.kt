package com.harunmaul.epelatihan.data.repository

import com.harunmaul.epelatihan.data.firebase.FirebaseReferences
import com.harunmaul.epelatihan.data.firebase.model.AppSettingsDocument
import com.harunmaul.epelatihan.data.mapper.toDomain
import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class DefaultAppSettingsRepository @Inject constructor(
    private val references: FirebaseReferences,
) : AppSettingsRepository {

    private val defaultSettings = AppSettings(
        communityName = DEFAULT_COMMUNITY_NAME,
        schemaVersion = 1,
        updatedBy = null,
        updatedAt = null,
    )

    override fun observeAppSettings(): Flow<AppSettings> = callbackFlow {
        val listener = references.appSettings.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(defaultSettings)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val doc = snapshot.toObject(AppSettingsDocument::class.java)
                val domain = doc?.toDomain() ?: defaultSettings
                val name = domain.communityName.trim().ifEmpty { DEFAULT_COMMUNITY_NAME }
                trySend(domain.copy(communityName = name))
            } else {
                trySend(defaultSettings)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getAppSettings(): AppSettings {
        return try {
            val snapshot = references.appSettings.get().await()
            if (snapshot.exists()) {
                val doc = snapshot.toObject(AppSettingsDocument::class.java)
                val domain = doc?.toDomain() ?: defaultSettings
                val name = domain.communityName.trim().ifEmpty { DEFAULT_COMMUNITY_NAME }
                domain.copy(communityName = name)
            } else {
                defaultSettings
            }
        } catch (_: Exception) {
            defaultSettings
        }
    }

    companion object {
        const val DEFAULT_COMMUNITY_NAME = "E-Pelatihan"
    }
}
