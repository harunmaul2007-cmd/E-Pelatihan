package com.harunmaul.epelatihan.domain.repository

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.model.WhatsAppOpenLog
import kotlinx.coroutines.flow.Flow

interface WhatsAppLogRepository {
    suspend fun recordOpenLog(
        actorId: String,
        registrationId: String,
        recipientPhone: String,
        messageSnapshot: String,
    ): AppResult<WhatsAppOpenLog>

    fun observeLogsByRegistration(registrationId: String): Flow<List<WhatsAppOpenLog>>
}
