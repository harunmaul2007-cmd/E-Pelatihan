package com.harunmaul.epelatihan.domain.validation

enum class ValidationField {
    COMMUNITY_NAME,
    DISPLAY_NAME,
    EMAIL,
    PASSWORD,
    TRAINING_NAME,
    START_AT,
    END_AT,
    TIME_ZONE,
    SPEAKER,
    ZOOM_URL,
    LOCATION,
    RECORDING_URL,
    MATERIAL_URL,
    NORMAL_PRICE,
    EARLY_BIRD_PRICE,
    PARTICIPANT_NAME,
    PHONE_NUMBER,
    REGISTRATION_ID,
    TRAINING_ID,
    PARTICIPANT_ID,
    PRICING_TYPE,
    PRICE_AMOUNT,
    PAYMENT_STATUS,
    PAID_AT,
    PAID_BY,
    ACTOR_ID,
}

enum class ValidationCode {
    REQUIRED,
    INVALID_FORMAT,
    TOO_LONG,
    TOO_SHORT,
    MUST_BE_AFTER,
    MUST_BE_HTTPS,
    MUST_BE_EMPTY,
    NEGATIVE_NOT_ALLOWED,
    MUST_BE_LESS_THAN_NORMAL,
    NOT_AVAILABLE,
    VALUE_MISMATCH,
    INVALID_STATE,
}

data class ValidationIssue(
    val field: ValidationField,
    val code: ValidationCode,
    val message: String,
)

sealed interface ValidationResult {
    data object Valid : ValidationResult

    data class Invalid(
        val issues: List<ValidationIssue>,
    ) : ValidationResult {
        init {
            require(issues.isNotEmpty()) { "ValidationResult.Invalid memerlukan minimal satu issue" }
        }
    }
}

fun List<ValidationIssue>.asValidationResult(): ValidationResult =
    if (isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(this)
