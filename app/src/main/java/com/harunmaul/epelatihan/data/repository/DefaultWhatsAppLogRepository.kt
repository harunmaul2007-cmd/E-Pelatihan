package com.harunmaul.epelatihan.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.data.firebase.FirebaseReferences
import com.harunmaul.epelatihan.data.firebase.model.WhatsAppOpenLogDocument
import com.harunmaul.epelatihan.data.mapper.toDomain
import com.harunmaul.epelatihan.domain.model.WhatsAppOpenLog
import com.harunmaul.epelatihan.domain.repository.WhatsAppLogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class DefaultWhatsAppLogRepository @Inject constructor(
    private val references: FirebaseReferences,
) : WhatsAppLogRepository {

    override suspend fun recordOpenLog(
        actorId: String,
        registrationId: String,
        recipientPhone: String,
        messageSnapshot: String,
    ): AppResult<WhatsAppOpenLog> {
        return try {
            val docRef = references.whatsappOpenLogs.document()
            val payload = mapOf(
                "registrationId" to registrationId,
                "recipientPhone" to recipientPhone,
                "messageSnapshot" to messageSnapshot,
                "openedBy" to actorId,
                "openedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()

            val log = WhatsAppOpenLog(
                id = docRef.id,
                registrationId = registrationId,
                recipientPhone = recipientPhone,
                messageSnapshot = messageSnapshot,
                openedBy = actorId,
                openedAt = null,
            )
            AppResult.Success(log)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override fun observeLogsByRegistration(registrationId: String): Flow<List<WhatsAppOpenLog>> = callbackFlow {
        val listener = references.whatsappOpenLogs
            .whereEqualTo("registrationId", registrationId)
            .orderBy("openedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val logs = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(WhatsAppOpenLogDocument::class.java)?.apply {
                            id = doc.id
                        }?.toDomain()
                    } catch (_: Exception) {
                        null
                    }
                }.orEmpty()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }
}
