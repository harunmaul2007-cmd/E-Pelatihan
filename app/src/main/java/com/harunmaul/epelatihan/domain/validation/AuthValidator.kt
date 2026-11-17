package com.harunmaul.epelatihan.domain.validation

object AuthValidator {
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_NAME_LENGTH = 100
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validateDisplayName(value: String): ValidationResult {
        val name = value.trim()
        val issues = buildList {
            if (name.isEmpty()) {
                add(
                    ValidationIssue(
                        field = ValidationField.DISPLAY_NAME,
                        code = ValidationCode.REQUIRED,
                        message = "Nama lengkap wajib diisi",
                    ),
                )
            } else if (name.length > MAX_NAME_LENGTH) {
                add(
                    ValidationIssue(
                        field = ValidationField.DISPLAY_NAME,
                        code = ValidationCode.TOO_LONG,
                        message = "Nama lengkap maksimal $MAX_NAME_LENGTH karakter",
                    ),
                )
            }
        }
        return issues.asValidationResult()
    }

    fun validateEmail(value: String): ValidationResult {
        val email = value.trim()
        val issues = buildList {
            if (email.isEmpty()) {
                add(
                    ValidationIssue(
                        field = ValidationField.EMAIL,
                        code = ValidationCode.REQUIRED,
                        message = "Email wajib diisi",
                    ),
                )
            } else if (!EMAIL_REGEX.matches(email)) {
                add(
                    ValidationIssue(
                        field = ValidationField.EMAIL,
                        code = ValidationCode.INVALID_FORMAT,
                        message = "Format email tidak valid",
                    ),
                )
            }
        }
        return issues.asValidationResult()
    }

    fun validatePassword(value: String): ValidationResult {
        val issues = buildList {
            if (value.isEmpty()) {
                add(
                    ValidationIssue(
                        field = ValidationField.PASSWORD,
                        code = ValidationCode.REQUIRED,
                        message = "Kata sandi wajib diisi",
                    ),
                )
            } else if (value.length < MIN_PASSWORD_LENGTH) {
                add(
                    ValidationIssue(
                        field = ValidationField.PASSWORD,
                        code = ValidationCode.TOO_SHORT,
                        message = "Kata sandi minimal $MIN_PASSWORD_LENGTH karakter",
                    ),
                )
            }
        }
        return issues.asValidationResult()
    }

    fun validateLogin(email: String, password: String): ValidationResult {
        val issues = buildList {
            val emailResult = validateEmail(email)
            if (emailResult is ValidationResult.Invalid) {
                addAll(emailResult.issues)
            }
            val passwordResult = validatePassword(password)
            if (passwordResult is ValidationResult.Invalid) {
                addAll(passwordResult.issues)
            }
        }
        return issues.asValidationResult()
    }
}
