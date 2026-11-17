package com.harunmaul.epelatihan.domain.validation

sealed interface MoneyParseResult {
    data class Success(val amount: Long) : MoneyParseResult
    data class Failure(val message: String) : MoneyParseResult
}

object MoneyAmountParser {
    fun parseRupiah(rawValue: String): MoneyParseResult {
        val withoutPrefix = rawValue.trim()
            .replace(Regex("^Rp\\s*", RegexOption.IGNORE_CASE), "")
        val digits = withoutPrefix
            .replace(".", "")
            .replace(" ", "")

        if (digits.isEmpty()) return MoneyParseResult.Failure("Nominal wajib diisi")
        if (',' in digits) return MoneyParseResult.Failure("Nominal Rupiah tidak menggunakan pecahan desimal")
        if (!digits.all { it in '0'..'9' }) {
            return MoneyParseResult.Failure("Nominal hanya boleh berisi angka dan pemisah ribuan")
        }

        val amount = digits.toLongOrNull()
            ?: return MoneyParseResult.Failure("Nominal terlalu besar")

        return MoneyParseResult.Success(amount)
    }
}
