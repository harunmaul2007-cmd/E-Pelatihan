package com.harunmaul.epelatihan.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.data.firebase.FirebaseReferences
import com.harunmaul.epelatihan.data.firebase.model.AuditLogDocument
import com.harunmaul.epelatihan.data.mapper.toDomain
import com.harunmaul.epelatihan.domain.model.AuditLog
import com.harunmaul.epelatihan.domain.repository.AuditLogRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class DefaultAuditLogRepository @Inject constructor(
    private val references: FirebaseReferences,
) : AuditLogRepository {

    override suspend fun recordLog(
        actorId: String,
        action: String,
        entityType: String,
        entityId: String,
        before: Map<String, Any?>,
        after: Map<String, Any?>,
    ): AppResult<Unit> {
        return try {
            val docRef = references.auditLogs.document()
            val payload = mapOf(
                "actorId" to actorId,
                "action" to action,
                "entityType" to entityType,
                "entityId" to entityId,
                "before" to before,
                "after" to after,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }

    override fun observeAuditLogs(limit: Int): Flow<List<AuditLog>> = callbackFlow {
        val listener = references.auditLogs
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val logs = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(AuditLogDocument::class.java)?.apply {
                            id = doc.id
                        }?.toDomain()
                    }
                    trySend(logs)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { listener.remove() }
    }
}
