package com.harunmaul.epelatihan.domain.validation

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

object ParticipantPhoneKeyFactory {
    fun create(phoneNormalized: String): String {
        require(PhoneNumberNormalizer.isNormalized(phoneNormalized)) {
            "phoneNormalized harus berupa nomor internasional tanpa tanda +"
        }
        return sha256(phoneNormalized)
    }
}

object RegistrationIdFactory {
    fun create(trainingId: String, participantId: String): String {
        require(trainingId.isNotBlank()) { "trainingId wajib tersedia" }
        require(participantId.isNotBlank()) { "participantId wajib tersedia" }
        return sha256("training:$trainingId|participant:$participantId")
    }
}

private fun sha256(value: String): String = MessageDigest
    .getInstance("SHA-256")
    .digest(value.toByteArray(StandardCharsets.UTF_8))
    .joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }
