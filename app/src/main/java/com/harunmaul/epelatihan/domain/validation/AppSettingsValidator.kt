package com.harunmaul.epelatihan.domain.validation

import com.harunmaul.epelatihan.domain.model.AppSettings

object AppSettingsValidator {
    const val MAX_COMMUNITY_NAME_LENGTH = 100

    fun normalizeCommunityName(value: String): String = value.trim()

    fun validate(settings: AppSettings): ValidationResult =
        validateCommunityName(settings.communityName)

    fun validateCommunityName(value: String): ValidationResult {
        val normalized = normalizeCommunityName(value)
        val issues = buildList {
            if (normalized.isEmpty()) {
                add(
                    ValidationIssue(
                        field = ValidationField.COMMUNITY_NAME,
                        code = ValidationCode.REQUIRED,
                        message = "Nama komunitas wajib diisi",
                    ),
                )
            }
            if (normalized.length > MAX_COMMUNITY_NAME_LENGTH) {
                add(
                    ValidationIssue(
                        field = ValidationField.COMMUNITY_NAME,
                        code = ValidationCode.TOO_LONG,
                        message = "Nama komunitas maksimal $MAX_COMMUNITY_NAME_LENGTH karakter",
                    ),
                )
            }
        }
        return issues.asValidationResult()
    }
}
