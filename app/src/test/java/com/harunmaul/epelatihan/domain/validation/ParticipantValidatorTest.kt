package com.harunmaul.epelatihan.domain.validation

import com.harunmaul.epelatihan.domain.model.Participant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParticipantValidatorTest {

    private fun sampleParticipant(
        name: String = "Budi Santoso",
        phoneNormalized: String = "628123456789",
        email: String? = null,
    ) = Participant(
        id = "p-1",
        name = name,
        nameSort = name.lowercase(),
        phoneDisplay = "08123456789",
        phoneNormalized = phoneNormalized,
        email = email,
        notes = "Catatan",
        active = true,
        createdBy = "admin-1",
        createdAt = null,
        updatedBy = "admin-1",
        updatedAt = null,
    )

    @Test
    fun `valid participant passes validation`() {
        val result = ParticipantValidator.validate(sampleParticipant())
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `blank name fails validation`() {
        val result = ParticipantValidator.validate(sampleParticipant(name = "   "))
        assertTrue(result is ValidationResult.Invalid)
        val issue = (result as ValidationResult.Invalid).issues.first()
        assertEquals(ValidationField.PARTICIPANT_NAME, issue.field)
        assertEquals(ValidationCode.REQUIRED, issue.code)
    }

    @Test
    fun `invalid phoneNormalized fails validation`() {
        val result = ParticipantValidator.validate(sampleParticipant(phoneNormalized = "+628123"))
        assertTrue(result is ValidationResult.Invalid)
        val issue = (result as ValidationResult.Invalid).issues.first()
        assertEquals(ValidationField.PHONE_NUMBER, issue.field)
        assertEquals(ValidationCode.INVALID_FORMAT, issue.code)
    }

    @Test
    fun `invalid email fails validation`() {
        val result = ParticipantValidator.validate(sampleParticipant(email = "budi-bukan-email"))
        assertTrue(result is ValidationResult.Invalid)
        val issue = (result as ValidationResult.Invalid).issues.first()
        assertEquals(ValidationField.EMAIL, issue.field)
        assertEquals(ValidationCode.INVALID_FORMAT, issue.code)
    }

    @Test
    fun `valid email passes validation`() {
        val result = ParticipantValidator.validate(sampleParticipant(email = "budi@example.com"))
        assertTrue(result is ValidationResult.Valid)
    }
}
