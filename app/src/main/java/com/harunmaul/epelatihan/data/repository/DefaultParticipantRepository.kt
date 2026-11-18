package com.harunmaul.epelatihan.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.DuplicateParticipantPhoneException
import com.harunmaul.epelatihan.data.firebase.FirebaseReferences
import com.harunmaul.epelatihan.data.firebase.model.ParticipantDocument
import com.harunmaul.epelatihan.data.mapper.toDomain
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.ParticipantDetailSnapshot
import com.harunmaul.epelatihan.domain.model.ParticipantListSnapshot
import com.harunmaul.epelatihan.domain.repository.AuditLogRepository
import com.harunmaul.epelatihan.domain.repository.ParticipantRepository
import com.harunmaul.epelatihan.domain.validation.ParticipantPhoneKeyFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class DefaultParticipantRepository @Inject constructor(
    private val references: FirebaseReferences,
    private val auditLogRepository: AuditLogRepository,
) : ParticipantRepository {

    override fun observeParticipants(includeInactive: Boolean): Flow<AppResult<ParticipantListSnapshot>> = callbackFlow {
        val query = if (includeInactive) {
            references.participants
        } else {
            references.participants
                .whereEqualTo("active", true)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(AppResult.Failure(error))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val participants = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(ParticipantDocument::class.java)?.apply {
                            id = doc.id
                        }?.toDomain()
                    } catch (_: Exception) {
                        null
                    }
                }.sortedBy { it.nameSort }
                val isFromCache = snapshot.metadata.isFromCache
                trySend(AppResult.Success(ParticipantListSnapshot(participants, isFromCache)))
            } else {
                trySend(AppResult.Success(ParticipantListSnapshot(emptyList(), false)))
            }
        }
        awaitClose { listener.remove() }
    }

    override fun observeParticipant(participantId: String): Flow<AppResult<ParticipantDetailSnapshot>> = callbackFlow {
        val listener = references.participants.document(participantId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(AppResult.Failure(error))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val participant = try {
                        snapshot.toObject(ParticipantDocument::class.java)?.apply {
                            id = snapshot.id
                        }?.toDomain()
                    } catch (_: Exception) {
                        null
                    }
                    val isFromCache = snapshot.metadata.isFromCache
                    trySend(AppResult.Success(ParticipantDetailSnapshot(participant, isFromCache)))
                } else {
                    trySend(AppResult.Success(ParticipantDetailSnapshot(null, false)))
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getParticipant(participantId: String): AppResult<Participant?> {
        return try {
            val snapshot = references.participants.document(participantId).get().await()
            if (!snapshot.exists()) {
                AppResult.Success(null)
            } else {
                val participant = snapshot.toObject(ParticipantDocument::class.java)?.apply {
                    id = snapshot.id
                }?.toDomain()
                AppResult.Success(participant)
            }
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun findParticipantByPhone(phoneNormalized: String): AppResult<Participant?> {
        return try {
            val phoneKey = ParticipantPhoneKeyFactory.create(phoneNormalized)
            val indexDoc = references.participantPhoneIndex.document(phoneKey).get().await()
            if (!indexDoc.exists()) {
                return AppResult.Success(null)
            }
            val participantId = indexDoc.getString("participantId")
            if (participantId.isNullOrBlank()) {
                return AppResult.Success(null)
            }
            val participantDoc = references.participants.document(participantId).get().await()
            if (!participantDoc.exists()) {
                return AppResult.Success(null)
            }
            val participant = participantDoc.toObject(ParticipantDocument::class.java)?.apply {
                id = participantDoc.id
            }?.toDomain()
            AppResult.Success(participant)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun createParticipant(participant: Participant): AppResult<Participant> {
        return try {
            val phoneKey = ParticipantPhoneKeyFactory.create(participant.phoneNormalized)
            val docRef = if (participant.id.isNotBlank()) {
                references.participants.document(participant.id)
            } else {
                references.participants.document()
            }
            val participantWithId = participant.copy(id = docRef.id)
            val phoneIndexRef = references.participantPhoneIndex.document(phoneKey)

            references.firestore.runTransaction { transaction ->
                val indexSnapshot = transaction.get(phoneIndexRef)
                if (indexSnapshot.exists()) {
                    throw DuplicateParticipantPhoneException(
                        "Nomor WhatsApp ${participant.phoneDisplay} sudah terdaftar.",
                    )
                }

                val payload = mapOf(
                    "name" to participant.name.trim(),
                    "nameSort" to participant.name.trim().lowercase(),
                    "phoneDisplay" to participant.phoneDisplay.trim(),
                    "phoneNormalized" to participant.phoneNormalized,
                    "email" to participant.email?.trim()?.ifBlank { null },
                    "notes" to participant.notes?.trim()?.ifBlank { null },
                    "active" to true,
                    "createdBy" to participant.createdBy,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedBy" to participant.createdBy,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                transaction.set(docRef, payload)

                val indexPayload = mapOf(
                    "participantId" to docRef.id,
                    "phoneNormalized" to participant.phoneNormalized,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
                transaction.set(phoneIndexRef, indexPayload)
            }.await()

            auditLogRepository.recordLog(
                actorId = participant.createdBy,
                action = "CREATE_PARTICIPANT",
                entityType = "PARTICIPANT",
                entityId = docRef.id,
                before = emptyMap(),
                after = mapOf(
                    "name" to participant.name,
                    "phoneNormalized" to participant.phoneNormalized,
                    "email" to participant.email,
                ),
            )

            AppResult.Success(participantWithId)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun updateParticipant(actorId: String, participant: Participant): AppResult<Participant> {
        return try {
            val docRef = references.participants.document(participant.id)
            var oldName = ""
            var oldPhoneNormalized = ""

            references.firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) {
                    throw NoSuchElementException("Peserta tidak ditemukan.")
                }
                oldName = snapshot.getString("name").orEmpty()
                oldPhoneNormalized = snapshot.getString("phoneNormalized").orEmpty()
                val newPhoneNormalized = participant.phoneNormalized

                if (oldPhoneNormalized.isNotBlank() && oldPhoneNormalized != newPhoneNormalized) {
                    val oldKey = ParticipantPhoneKeyFactory.create(oldPhoneNormalized)
                    val newKey = ParticipantPhoneKeyFactory.create(newPhoneNormalized)
                    val newIndexRef = references.participantPhoneIndex.document(newKey)
                    val newIndexSnap = transaction.get(newIndexRef)

                    if (newIndexSnap.exists()) {
                        val existingParticipantId = newIndexSnap.getString("participantId")
                        if (existingParticipantId != participant.id) {
                            throw DuplicateParticipantPhoneException(
                                "Nomor WhatsApp ${participant.phoneDisplay} sudah terdaftar.",
                            )
                        }
                    }

                    val oldIndexRef = references.participantPhoneIndex.document(oldKey)
                    transaction.delete(oldIndexRef)

                    val indexPayload = mapOf(
                        "participantId" to participant.id,
                        "phoneNormalized" to newPhoneNormalized,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                    transaction.set(newIndexRef, indexPayload)
                }

                val payload = mapOf(
                    "name" to participant.name.trim(),
                    "nameSort" to participant.name.trim().lowercase(),
                    "phoneDisplay" to participant.phoneDisplay.trim(),
                    "phoneNormalized" to participant.phoneNormalized,
                    "email" to participant.email?.trim()?.ifBlank { null },
                    "notes" to participant.notes?.trim()?.ifBlank { null },
                    "updatedBy" to actorId,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                transaction.update(docRef, payload)
            }.await()

            auditLogRepository.recordLog(
                actorId = actorId,
                action = "UPDATE_PARTICIPANT",
                entityType = "PARTICIPANT",
                entityId = participant.id,
                before = mapOf(
                    "name" to oldName,
                    "phoneNormalized" to oldPhoneNormalized,
                ),
                after = mapOf(
                    "name" to participant.name,
                    "phoneNormalized" to participant.phoneNormalized,
                    "email" to participant.email,
                ),
            )

            AppResult.Success(participant)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun archiveParticipant(actorId: String, participantId: String): AppResult<Unit> {
        return try {
            val docRef = references.participants.document(participantId)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                return AppResult.Failure(NoSuchElementException("Peserta tidak ditemukan."))
            }

            val payload = mapOf(
                "active" to false,
                "updatedBy" to actorId,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.update(payload).await()

            auditLogRepository.recordLog(
                actorId = actorId,
                action = "ARCHIVE_PARTICIPANT",
                entityType = "PARTICIPANT",
                entityId = participantId,
                before = mapOf("active" to true),
                after = mapOf("active" to false),
            )

            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }
}
