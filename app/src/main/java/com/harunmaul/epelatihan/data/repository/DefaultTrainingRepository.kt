package com.harunmaul.epelatihan.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.data.firebase.FirebaseReferences
import com.harunmaul.epelatihan.data.firebase.model.TrainingDocument
import com.harunmaul.epelatihan.data.mapper.toDocument
import com.harunmaul.epelatihan.data.mapper.toDomain
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingDetailSnapshot
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.TrainingListSnapshot
import com.harunmaul.epelatihan.domain.repository.AuditLogRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class DefaultTrainingRepository @Inject constructor(
    private val references: FirebaseReferences,
    private val auditLogRepository: AuditLogRepository,
) : TrainingRepository {

    override fun observeTrainings(includeArchived: Boolean): Flow<AppResult<TrainingListSnapshot>> = callbackFlow {
        val query = if (includeArchived) {
            references.trainings
        } else {
            references.trainings
                .whereEqualTo("lifecycle", TrainingLifecycle.ACTIVE.name)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppResult.Failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val trainings = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(TrainingDocument::class.java)?.apply {
                            id = doc.id
                        }?.toDomain()
                    } catch (_: Exception) {
                        null
                    }
                }.sortedBy { it.startAt }
                val isFromCache = snapshot.metadata.isFromCache
                trySend(AppResult.Success(TrainingListSnapshot(trainings, isFromCache)))
            } else {
                trySend(AppResult.Success(TrainingListSnapshot(emptyList(), false)))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun observeTraining(trainingId: String): Flow<AppResult<TrainingDetailSnapshot>> = callbackFlow {
        val listener = references.trainings.document(trainingId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val training = try {
                        snapshot.toObject(TrainingDocument::class.java)?.apply {
                            id = snapshot.id
                        }?.toDomain()
                    } catch (_: Exception) {
                        null
                    }
                    val isFromCache = snapshot.metadata.isFromCache
                    trySend(AppResult.Success(TrainingDetailSnapshot(training, isFromCache)))
                } else {
                    trySend(AppResult.Success(TrainingDetailSnapshot(null, false)))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getTraining(trainingId: String): AppResult<Training?> {
        return try {
            val snapshot = references.trainings.document(trainingId).get().await()
            if (snapshot.exists()) {
                val training = snapshot.toObject(TrainingDocument::class.java)?.apply {
                    id = snapshot.id
                }?.toDomain()
                AppResult.Success(training)
            } else {
                AppResult.Success(null)
            }
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun createTraining(training: Training): AppResult<Training> {
        return try {
            val docRef = references.trainings.document()
            val trainingWithId = training.copy(id = docRef.id)
            val doc = trainingWithId.toDocument()

            val payload = mapOf(
                "name" to doc.name,
                "description" to doc.description,
                "startAt" to doc.startAt,
                "endAt" to doc.endAt,
                "timeZoneId" to doc.timeZoneId,
                "speaker" to doc.speaker,
                "meetingMethod" to doc.meetingMethod,
                "zoomUrl" to doc.zoomUrl,
                "location" to doc.location,
                "recordingUrl" to doc.recordingUrl,
                "materialUrl" to doc.materialUrl,
                "pricingScheme" to doc.pricingScheme,
                "normalPrice" to doc.normalPrice,
                "earlyBirdPrice" to doc.earlyBirdPrice,
                "lifecycle" to doc.lifecycle,
                "createdBy" to doc.createdBy,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedBy" to doc.updatedBy,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()

            auditLogRepository.recordLog(
                actorId = training.createdBy,
                action = "CREATE_TRAINING",
                entityType = "TRAINING",
                entityId = docRef.id,
                before = emptyMap(),
                after = mapOf(
                    "name" to doc.name,
                    "speaker" to doc.speaker,
                    "meetingMethod" to doc.meetingMethod,
                    "pricingScheme" to doc.pricingScheme,
                    "normalPrice" to doc.normalPrice,
                    "earlyBirdPrice" to doc.earlyBirdPrice,
                ),
            )

            AppResult.Success(trainingWithId)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun updateTraining(actorId: String, training: Training): AppResult<Training> {
        return try {
            val docRef = references.trainings.document(training.id)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return AppResult.Failure(IllegalStateException("Pelatihan tidak ditemukan."))
            }

            val beforeDoc = snapshot.toObject(TrainingDocument::class.java)
            val doc = training.toDocument()

            val updates = mapOf(
                "name" to doc.name,
                "description" to doc.description,
                "startAt" to doc.startAt,
                "endAt" to doc.endAt,
                "timeZoneId" to doc.timeZoneId,
                "speaker" to doc.speaker,
                "meetingMethod" to doc.meetingMethod,
                "zoomUrl" to doc.zoomUrl,
                "location" to doc.location,
                "recordingUrl" to doc.recordingUrl,
                "materialUrl" to doc.materialUrl,
                "pricingScheme" to doc.pricingScheme,
                "normalPrice" to doc.normalPrice,
                "earlyBirdPrice" to doc.earlyBirdPrice,
                "updatedBy" to actorId,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.update(updates).await()

            auditLogRepository.recordLog(
                actorId = actorId,
                action = "UPDATE_TRAINING",
                entityType = "TRAINING",
                entityId = training.id,
                before = mapOf(
                    "name" to (beforeDoc?.name ?: ""),
                    "speaker" to (beforeDoc?.speaker ?: ""),
                    "meetingMethod" to (beforeDoc?.meetingMethod ?: ""),
                    "normalPrice" to (beforeDoc?.normalPrice ?: 0),
                    "earlyBirdPrice" to beforeDoc?.earlyBirdPrice,
                ),
                after = mapOf(
                    "name" to doc.name,
                    "speaker" to doc.speaker,
                    "meetingMethod" to doc.meetingMethod,
                    "normalPrice" to doc.normalPrice,
                    "earlyBirdPrice" to doc.earlyBirdPrice,
                ),
            )

            AppResult.Success(training)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun archiveTraining(actorId: String, trainingId: String): AppResult<Unit> {
        return try {
            val docRef = references.trainings.document(trainingId)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return AppResult.Failure(IllegalStateException("Pelatihan tidak ditemukan."))
            }
            val beforeLifecycle = snapshot.getString("lifecycle") ?: TrainingLifecycle.ACTIVE.name

            docRef.update(
                mapOf(
                    "lifecycle" to TrainingLifecycle.ARCHIVED.name,
                    "updatedBy" to actorId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()

            auditLogRepository.recordLog(
                actorId = actorId,
                action = "ARCHIVE_TRAINING",
                entityType = "TRAINING",
                entityId = trainingId,
                before = mapOf("lifecycle" to beforeLifecycle),
                after = mapOf("lifecycle" to TrainingLifecycle.ARCHIVED.name),
            )

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }
}
