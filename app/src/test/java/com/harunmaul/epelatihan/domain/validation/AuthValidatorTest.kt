package com.harunmaul.epelatihan.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun `validateEmail with valid email returns Valid`() {
        val result = AuthValidator.validateEmail("user@example.com")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateEmail with empty email returns REQUIRED`() {
        val result = AuthValidator.validateEmail("   ")
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(1, invalid.issues.size)
        assertEquals(ValidationField.EMAIL, invalid.issues[0].field)
        assertEquals(ValidationCode.REQUIRED, invalid.issues[0].code)
    }

    @Test
    fun `validateEmail with invalid format returns INVALID_FORMAT`() {
        val result = AuthValidator.validateEmail("invalid-email")
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(1, invalid.issues.size)
        assertEquals(ValidationField.EMAIL, invalid.issues[0].field)
        assertEquals(ValidationCode.INVALID_FORMAT, invalid.issues[0].code)
    }

    @Test
    fun `validatePassword with valid password returns Valid`() {
        val result = AuthValidator.validatePassword("123456")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validatePassword with empty password returns REQUIRED`() {
        val result = AuthValidator.validatePassword("")
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(1, invalid.issues.size)
        assertEquals(ValidationField.PASSWORD, invalid.issues[0].field)
        assertEquals(ValidationCode.REQUIRED, invalid.issues[0].code)
    }

    @Test
    fun `validatePassword with less than 6 chars returns TOO_SHORT`() {
        val result = AuthValidator.validatePassword("12345")
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(1, invalid.issues.size)
        assertEquals(ValidationField.PASSWORD, invalid.issues[0].field)
        assertEquals(ValidationCode.TOO_SHORT, invalid.issues[0].code)
    }

    @Test
    fun `validateLogin with both valid returns Valid`() {
        val result = AuthValidator.validateLogin("admin@lembaga.org", "rahasia123")
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateLogin with both invalid returns issues for both`() {
        val result = AuthValidator.validateLogin("", "123")
        assertTrue(result is ValidationResult.Invalid)
        val invalid = result as ValidationResult.Invalid
        assertEquals(2, invalid.issues.size)
        assertEquals(ValidationField.EMAIL, invalid.issues[0].field)
        assertEquals(ValidationField.PASSWORD, invalid.issues[1].field)
    }
}
