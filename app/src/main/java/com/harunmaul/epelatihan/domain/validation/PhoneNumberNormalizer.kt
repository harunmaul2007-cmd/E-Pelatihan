package com.harunmaul.epelatihan.domain.validation

sealed interface PhoneNormalizationResult {
    data class Success(val normalized: String) : PhoneNormalizationResult
    data class Failure(val message: String) : PhoneNormalizationResult
}

object PhoneNumberNormalizer {
    private const val INDONESIA_COUNTRY_CODE = "62"
    private const val MIN_E164_DIGITS = 8
    private const val MAX_E164_DIGITS = 15
    private const val MIN_INDONESIA_SUBSCRIBER_DIGITS = 8
    private const val MAX_INDONESIA_SUBSCRIBER_DIGITS = 13
    private val allowedSeparators = setOf(' ', '-', '(', ')', '.')

    fun normalize(rawValue: String): PhoneNormalizationResult {
        val raw = rawValue.trim()
        if (raw.isEmpty()) return PhoneNormalizationResult.Failure("Nomor WhatsApp wajib diisi")
        if (raw.any(Char::isLetter)) {
            return PhoneNormalizationResult.Failure("Nomor WhatsApp tidak boleh mengandung huruf")
        }
        if (raw.any { character ->
                character !in '0'..'9' && character != '+' && character !in allowedSeparators
            }
        ) {
            return PhoneNormalizationResult.Failure("Nomor WhatsApp mengandung karakter yang tidak valid")
        }
        if (raw.count { it == '+' } > 1 || ('+' in raw && !raw.startsWith('+'))) {
            return PhoneNormalizationResult.Failure("Tanda + hanya boleh berada di awal nomor")
        }

        val digits = raw.filter { it in '0'..'9' }
        val normalized = when {
            raw.startsWith('+') -> digits
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith(INDONESIA_COUNTRY_CODE) -> digits
            digits.startsWith('0') -> INDONESIA_COUNTRY_CODE + digits.drop(1)
            digits.startsWith('8') -> INDONESIA_COUNTRY_CODE + digits
            else -> {
                return PhoneNormalizationResult.Failure(
                    "Gunakan awalan 0, 8, +62, 62, atau kode negara dengan tanda +",
                )
            }
        }

        if (normalized.length !in MIN_E164_DIGITS..MAX_E164_DIGITS || normalized.startsWith('0')) {
            return PhoneNormalizationResult.Failure("Panjang nomor WhatsApp tidak valid")
        }

        if (normalized.startsWith(INDONESIA_COUNTRY_CODE)) {
            val subscriber = normalized.removePrefix(INDONESIA_COUNTRY_CODE)
            if (subscriber.length !in MIN_INDONESIA_SUBSCRIBER_DIGITS..MAX_INDONESIA_SUBSCRIBER_DIGITS ||
                subscriber.startsWith('0')
            ) {
                return PhoneNormalizationResult.Failure("Nomor Indonesia tidak valid")
            }
        }

        return PhoneNormalizationResult.Success(normalized)
    }

    fun isNormalized(value: String): Boolean =
        when (val result = normalize("+$value")) {
            is PhoneNormalizationResult.Success -> result.normalized == value
            is PhoneNormalizationResult.Failure -> false
        }
}
