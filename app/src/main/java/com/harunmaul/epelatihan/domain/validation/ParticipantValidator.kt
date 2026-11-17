package com.harunmaul.epelatihan.domain.validation

import com.harunmaul.epelatihan.domain.model.Participant

object ParticipantValidator {
    fun validate(participant: Participant): ValidationResult {
        val issues = buildList {
            if (participant.name.isBlank()) {
                add(
                    ValidationIssue(
                        field = ValidationField.PARTICIPANT_NAME,
                        code = ValidationCode.REQUIRED,
                        message = "Nama peserta wajib diisi",
                    ),
                )
            }

            val phoneNormalized = participant.phoneNormalized
            if (!phoneNormalized.all { it in '0'..'9' } ||
                !PhoneNumberNormalizer.isNormalized(phoneNormalized)
            ) {
                add(
                    ValidationIssue(
                        field = ValidationField.PHONE_NUMBER,
                        code = ValidationCode.INVALID_FORMAT,
                        message = "Nomor peserta harus berupa digit internasional tanpa tanda +",
                    ),
                )
            }
        }

        return issues.asValidationResult()
    }
}
