package com.harunmaul.epelatihan.domain.validation

import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.Training
import java.time.DateTimeException
import java.time.ZoneId

object TrainingValidator {
    fun validate(training: Training): ValidationResult {
        val issues = buildList {
            if (training.name.isBlank()) {
                add(required(ValidationField.TRAINING_NAME, "Nama pelatihan wajib diisi"))
            }
            if (training.speaker.isBlank()) {
                add(required(ValidationField.SPEAKER, "Nama pemateri wajib diisi"))
            }
            if (!training.endAt.isAfter(training.startAt)) {
                add(
                    ValidationIssue(
                        field = ValidationField.END_AT,
                        code = ValidationCode.MUST_BE_AFTER,
                        message = "Waktu selesai harus setelah waktu mulai",
                    ),
                )
            }
            if (!isValidZoneId(training.timeZoneId)) {
                add(
                    ValidationIssue(
                        field = ValidationField.TIME_ZONE,
                        code = ValidationCode.INVALID_FORMAT,
                        message = "Zona waktu tidak valid",
                    ),
                )
            }

            when (training.meetingMethod) {
                MeetingMethod.ONLINE -> validateOnlineFields(training, this)
                MeetingMethod.OFFLINE -> validateOfflineFields(training, this)
            }

            validateOptionalUrl(
                value = training.recordingUrl,
                field = ValidationField.RECORDING_URL,
                label = "Link rekaman",
                issues = this,
            )
            validateOptionalUrl(
                value = training.materialUrl,
                field = ValidationField.MATERIAL_URL,
                label = "Link materi",
                issues = this,
            )

            if (training.normalPrice < 0) {
                add(negativePrice(ValidationField.NORMAL_PRICE, "Harga normal"))
            }

            when (training.pricingScheme) {
                PricingScheme.NORMAL_ONLY -> {
                    if (training.earlyBirdPrice != null) {
                        add(
                            ValidationIssue(
                                field = ValidationField.EARLY_BIRD_PRICE,
                                code = ValidationCode.MUST_BE_EMPTY,
                                message = "Harga early bird harus kosong untuk skema harga normal",
                            ),
                        )
                    }
                }

                PricingScheme.EARLY_BIRD_AND_NORMAL -> {
                    val earlyBirdPrice = training.earlyBirdPrice
                    if (earlyBirdPrice == null) {
                        add(required(ValidationField.EARLY_BIRD_PRICE, "Harga early bird wajib diisi"))
                    } else {
                        if (earlyBirdPrice < 0) {
                            add(negativePrice(ValidationField.EARLY_BIRD_PRICE, "Harga early bird"))
                        }
                        if (earlyBirdPrice >= training.normalPrice) {
                            add(
                                ValidationIssue(
                                    field = ValidationField.EARLY_BIRD_PRICE,
                                    code = ValidationCode.MUST_BE_LESS_THAN_NORMAL,
                                    message = "Harga early bird harus lebih kecil daripada harga normal",
                                ),
                            )
                        }
                    }
                }
            }
        }

        return issues.asValidationResult()
    }

    private fun validateOnlineFields(
        training: Training,
        issues: MutableList<ValidationIssue>,
    ) {
        val zoomUrl = training.zoomUrl
        if (zoomUrl.isNullOrBlank()) {
            issues += required(ValidationField.ZOOM_URL, "Link Zoom wajib untuk pelatihan online")
        } else if (!HttpsUrlValidator.isValid(zoomUrl)) {
            issues += invalidHttps(ValidationField.ZOOM_URL, "Link Zoom")
        }

        if (!training.location.isNullOrBlank()) {
            issues += ValidationIssue(
                field = ValidationField.LOCATION,
                code = ValidationCode.MUST_BE_EMPTY,
                message = "Lokasi harus kosong untuk pelatihan online",
            )
        }
    }

    private fun validateOfflineFields(
        training: Training,
        issues: MutableList<ValidationIssue>,
    ) {
        if (training.location.isNullOrBlank()) {
            issues += required(ValidationField.LOCATION, "Lokasi wajib untuk pelatihan offline")
        }
        if (!training.zoomUrl.isNullOrBlank()) {
            issues += ValidationIssue(
                field = ValidationField.ZOOM_URL,
                code = ValidationCode.MUST_BE_EMPTY,
                message = "Link Zoom harus kosong untuk pelatihan offline",
            )
        }
    }

    private fun validateOptionalUrl(
        value: String?,
        field: ValidationField,
        label: String,
        issues: MutableList<ValidationIssue>,
    ) {
        if (!value.isNullOrBlank() && !HttpsUrlValidator.isValid(value)) {
            issues += invalidHttps(field, label)
        }
    }

    private fun isValidZoneId(value: String): Boolean =
        try {
            value.isNotBlank() && ZoneId.of(value) != null
        } catch (_: DateTimeException) {
            false
        }

    private fun required(field: ValidationField, message: String) = ValidationIssue(
        field = field,
        code = ValidationCode.REQUIRED,
        message = message,
    )

    private fun invalidHttps(field: ValidationField, label: String) = ValidationIssue(
        field = field,
        code = ValidationCode.MUST_BE_HTTPS,
        message = "$label harus berupa URL HTTPS yang valid",
    )

    private fun negativePrice(field: ValidationField, label: String) = ValidationIssue(
        field = field,
        code = ValidationCode.NEGATIVE_NOT_ALLOWED,
        message = "$label tidak boleh negatif",
    )
}
