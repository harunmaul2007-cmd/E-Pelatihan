package com.harunmaul.epelatihan.feature.participant.importwa

import com.harunmaul.epelatihan.domain.validation.PhoneNormalizationResult
import com.harunmaul.epelatihan.domain.validation.PhoneNumberNormalizer
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

data class ParsedContactCandidate(
    val originalLine: String = "",
    val name: String,
    val rawPhone: String,
    val normalizedPhone: String,
    val isValidPhone: Boolean,
    val isAlreadyRegistered: Boolean = false,
    val isSelected: Boolean = true,
)

object WhatsAppContactParser {

    private val phoneRegex = Regex("""(?:\+?62|08|8)[0-9\s()\-]{7,20}""")

    /**
     * Parse multiline text (e.g. copied from WhatsApp chat, group member list, or form messages).
     * Examples of supported line formats:
     * 1. Budi Santoso - 08123456789
     * 2. Siti Rahma (08567891234)
     * +628129876543 - Ahmad Fauzi
     * 081234567890 : Joko
     * Budi, 08123456789, budi@gmail.com
     */
    fun parseText(text: String): List<ParsedContactCandidate> {
        val lines = text.lines()
        val results = mutableListOf<ParsedContactCandidate>()
        val seenPhones = mutableSetOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isBlank()) continue

            val match = phoneRegex.find(trimmed)
            if (match != null) {
                val phoneFound = match.value.trim()
                val normResult = PhoneNumberNormalizer.normalize(phoneFound)
                val isValid = normResult is PhoneNormalizationResult.Success
                val normalized = if (isValid) {
                    (normResult as PhoneNormalizationResult.Success).normalized
                } else {
                    phoneFound.filter { it.isDigit() }
                }

                // Skip duplicates within the same batch
                if (normalized.isNotBlank() && seenPhones.contains(normalized)) {
                    continue
                }
                if (normalized.isNotBlank()) {
                    seenPhones.add(normalized)
                }

                // Extract name by removing the phone match and common prefixes/delimiters
                var cleanedName = trimmed.removeRange(match.range).trim()
                cleanedName = cleanedName
                    .replace(Regex("""^[0-9]+[.)\s-]+"""), "") // Remove leading numbers like "1. " or "2) "
                    .replace(Regex("""^[-–—:,/|\s]+"""), "")
                    .replace(Regex("""[-–—:,/|\s]+$"""), "")
                    .replace(Regex("""[()\[\]]"""), "")
                    .trim()

                if (cleanedName.isBlank()) {
                    val lastDigits = if (normalized.length >= 4) normalized.takeLast(4) else normalized
                    cleanedName = "Peserta WA ($lastDigits)"
                }

                results.add(
                    ParsedContactCandidate(
                        originalLine = trimmed,
                        name = cleanedName,
                        rawPhone = phoneFound,
                        normalizedPhone = normalized,
                        isValidPhone = isValid,
                        isSelected = isValid,
                    ),
                )
            }
        }
        return results
    }

    /**
     * Parse VCF (vCard) file stream (e.g. exported from WhatsApp or shared contacts).
     */
    fun parseVCard(inputStream: InputStream): List<ParsedContactCandidate> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val results = mutableListOf<ParsedContactCandidate>()
        val seenPhones = mutableSetOf<String>()

        var currentName: String? = null
        var currentPhone: String? = null

        reader.forEachLine { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> {
                    currentName = null
                    currentPhone = null
                }
                trimmed.startsWith("FN:", ignoreCase = true) -> {
                    currentName = trimmed.substring(3).trim()
                }
                trimmed.startsWith("N:", ignoreCase = true) && currentName == null -> {
                    val parts = trimmed.substring(2).split(";")
                    val lastName = parts.getOrNull(0)?.trim().orEmpty()
                    val firstName = parts.getOrNull(1)?.trim().orEmpty()
                    currentName = "$firstName $lastName".trim().ifBlank { null }
                }
                trimmed.startsWith("TEL", ignoreCase = true) -> {
                    val colonIndex = trimmed.indexOf(':')
                    if (colonIndex != -1) {
                        currentPhone = trimmed.substring(colonIndex + 1).trim()
                    }
                }
                trimmed.startsWith("END:VCARD", ignoreCase = true) -> {
                    val phone = currentPhone
                    if (!phone.isNullOrBlank()) {
                        val normResult = PhoneNumberNormalizer.normalize(phone)
                        val isValid = normResult is PhoneNormalizationResult.Success
                        val normalized = if (isValid) {
                            (normResult as PhoneNormalizationResult.Success).normalized
                        } else {
                            phone.filter { it.isDigit() }
                        }

                        if (!seenPhones.contains(normalized)) {
                            seenPhones.add(normalized)
                            val name = currentName?.ifBlank { null }
                                ?: ("Peserta WA (" + normalized.takeLast(4) + ")")

                            results.add(
                                ParsedContactCandidate(
                                    originalLine = "$name : $phone",
                                    name = name,
                                    rawPhone = phone,
                                    normalizedPhone = normalized,
                                    isValidPhone = isValid,
                                    isSelected = isValid,
                                ),
                            )
                        }
                    }
                    currentName = null
                    currentPhone = null
                }
            }
        }
        return results
    }
}
