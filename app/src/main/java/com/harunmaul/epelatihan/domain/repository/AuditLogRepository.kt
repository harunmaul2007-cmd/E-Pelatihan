package com.harunmaul.epelatihan.domain.repository

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.AuditLog
import kotlinx.coroutines.flow.Flow

interface AuditLogRepository {
    suspend fun recordLog(
        actorId: String,
        action: String,
        entityType: String,
        entityId: String,
        before: Map<String, Any?> = emptyMap(),
        after: Map<String, Any?> = emptyMap(),
    ): AppResult<Unit>

    fun observeAuditLogs(limit: Int = 100): Flow<List<AuditLog>>
}
