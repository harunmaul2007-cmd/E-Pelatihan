package com.harunmaul.epelatihan.data.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.domain.repository.WhatsAppLauncher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidWhatsAppLauncher @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : WhatsAppLauncher {

    override fun openWhatsApp(phoneNormalized: String, message: String): AppResult<Unit> {
        return try {
            if (phoneNormalized.isBlank()) {
                return AppResult.Failure(IllegalArgumentException("Nomor telepon tidak valid."))
            }

            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val uri = Uri.parse("https://wa.me/$phoneNormalized?text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            AppResult.Success(Unit)
        } catch (_: ActivityNotFoundException) {
            AppResult.Failure(IllegalStateException("WhatsApp tidak dapat dibuka pada perangkat ini."))
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }
}
