package com.harunmaul.epelatihan.domain.repository

import com.harunmaul.epelatihan.core.common.AppResult

interface WhatsAppLauncher {
    fun openWhatsApp(phoneNormalized: String, message: String): AppResult<Unit>
}
