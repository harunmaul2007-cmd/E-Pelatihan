package com.harunmaul.epelatihan.domain.validation

import java.net.URI

object HttpsUrlValidator {
    fun isValid(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return false

        return runCatching {
            val uri = URI(trimmed)
            uri.isAbsolute &&
                uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null
        }.getOrDefault(false)
    }
}
