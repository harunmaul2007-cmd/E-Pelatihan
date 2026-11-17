package com.harunmaul.epelatihan.data.mapper

import com.google.firebase.Timestamp
import com.harunmaul.epelatihan.data.firebase.FirestoreMappingException
import com.harunmaul.epelatihan.data.firebase.model.AppSettingsDocument
import com.harunmaul.epelatihan.data.firebase.model.AuditLogDocument
import com.harunmaul.epelatihan.data.firebase.model.ParticipantDocument
import com.harunmaul.epelatihan.data.firebase.model.RegistrationDocument
import com.harunmaul.epelatihan.data.firebase.model.TrainingDocument
import com.harunmaul.epelatihan.data.firebase.model.UserProfileDocument
import com.harunmaul.epelatihan.data.firebase.model.WhatsAppOpenLogDocument
import com.harunmaul.epelatihan.domain.model.AppSettings
import com.harunmaul.epelatihan.domain.model.AuditLog
import com.harunmaul.epelatihan.domain.model.InvitationStatus
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.Participant
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.Registration
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.UserProfile
import com.harunmaul.epelatihan.domain.model.UserRole
import com.harunmaul.epelatihan.domain.model.WhatsAppOpenLog
import java.time.Instant
import java.util.Date

fun AppSettingsDocument.toDomain(): AppSettings = AppSettings(
    communityName = communityName,
    logoUrl = logoUrl,
    logoStoragePath = logoStoragePath,
    schemaVersion = schemaVersion,
    updatedBy = updatedBy,
    updatedAt = updatedAt.toInstantOrNull(),
)

fun AppSettings.toDocument(): AppSettingsDocument = AppSettingsDocument(
    communityName = communityName,
    logoUrl = logoUrl,
    logoStoragePath = logoStoragePath,
    schemaVersion = schemaVersion,
    updatedBy = updatedBy,
    updatedAt = updatedAt.toTimestampOrNull(),
)

fun UserProfileDocument.toDomain(): UserProfile = UserProfile(
    id = id.requireValue("users.id"),
    displayName = displayName,
    email = email,
    role = role.enumValue("users.role"),
    active = active,
    invitationStatus = invitationStatus.enumValue("users.invitationStatus"),
    createdBy = createdBy,
    createdAt = createdAt.toInstantOrNull(),
    updatedBy = updatedBy,
    updatedAt = updatedAt.toInstantOrNull(),
)

fun UserProfile.toDocument(): UserProfileDocument = UserProfileDocument(
    id = id,
    displayName = displayName,
    email = email,
    role = role.name,
    active = active,
    invitationStatus = invitationStatus.name,
    createdBy = createdBy,
    createdAt = createdAt.toTimestampOrNull(),
    updatedBy = updatedBy,
    updatedAt = updatedAt.toTimestampOrNull(),
)

fun TrainingDocument.toDomain(): Training = Training(
    id = id.requireValue("trainings.id"),
    name = name,
    description = description,
    startAt = startAt.requireInstant("trainings.startAt"),
    endAt = endAt.requireInstant("trainings.endAt"),
    timeZoneId = timeZoneId,
    speaker = speaker,
    meetingMethod = meetingMethod.enumValue("trainings.meetingMethod"),
    zoomUrl = zoomUrl,
    location = location,
    recordingUrl = recordingUrl,
    materialUrl = materialUrl,
    pricingScheme = pricingScheme.enumValue("trainings.pricingScheme"),
    normalPrice = normalPrice,
    earlyBirdPrice = earlyBirdPrice,
    lifecycle = lifecycle.enumValue("trainings.lifecycle"),
    createdBy = createdBy,
    createdAt = createdAt.toInstantOrNull(),
    updatedBy = updatedBy,
    updatedAt = updatedAt.toInstantOrNull(),
)

fun Training.toDocument(): TrainingDocument = TrainingDocument(
    id = id,
    name = name,
    description = description,
    startAt = startAt.toTimestamp(),
    endAt = endAt.toTimestamp(),
    timeZoneId = timeZoneId,
    speaker = speaker,
    meetingMethod = meetingMethod.name,
    zoomUrl = zoomUrl,
    location = location,
    recordingUrl = recordingUrl,
    materialUrl = materialUrl,
    pricingScheme = pricingScheme.name,
    normalPrice = normalPrice,
    earlyBirdPrice = earlyBirdPrice,
    lifecycle = lifecycle.name,
    createdBy = createdBy,
    createdAt = createdAt.toTimestampOrNull(),
    updatedBy = updatedBy,
    updatedAt = updatedAt.toTimestampOrNull(),
)

fun ParticipantDocument.toDomain(): Participant = Participant(
    id = id.requireValue("participants.id"),
    name = name,
    nameSort = nameSort,
    phoneDisplay = phoneDisplay,
    phoneNormalized = phoneNormalized,
    email = email,
    notes = notes,
    active = active,
    createdBy = createdBy,
    createdAt = createdAt.toInstantOrNull(),
    updatedBy = updatedBy,
    updatedAt = updatedAt.toInstantOrNull(),
)

fun Participant.toDocument(): ParticipantDocument = ParticipantDocument(
    id = id,
    name = name,
    nameSort = nameSort,
    phoneDisplay = phoneDisplay,
    phoneNormalized = phoneNormalized,
    email = email,
    notes = notes,
    active = active,
    createdBy = createdBy,
    createdAt = createdAt.toTimestampOrNull(),
    updatedBy = updatedBy,
    updatedAt = updatedAt.toTimestampOrNull(),
)

fun RegistrationDocument.toDomain(): Registration = Registration(
    id = id.requireValue("registrations.id"),
    trainingId = trainingId,
    participantId = participantId,
    pricingType = pricingType.enumValue("registrations.pricingType"),
    priceAmount = priceAmount,
    paymentStatus = paymentStatus.enumValue("registrations.paymentStatus"),
    paidAt = paidAt.toInstantOrNull(),
    paidBy = paidBy,
    createdBy = createdBy,
    createdAt = createdAt.toInstantOrNull(),
    updatedBy = updatedBy,
    updatedAt = updatedAt.toInstantOrNull(),
)

fun Registration.toDocument(): RegistrationDocument = RegistrationDocument(
    id = id,
    trainingId = trainingId,
    participantId = participantId,
    pricingType = pricingType.name,
    priceAmount = priceAmount,
    paymentStatus = paymentStatus.name,
    paidAt = paidAt.toTimestampOrNull(),
    paidBy = paidBy,
    createdBy = createdBy,
    createdAt = createdAt.toTimestampOrNull(),
    updatedBy = updatedBy,
    updatedAt = updatedAt.toTimestampOrNull(),
)

fun WhatsAppOpenLogDocument.toDomain(): WhatsAppOpenLog = WhatsAppOpenLog(
    id = id.requireValue("whatsappOpenLogs.id"),
    registrationId = registrationId,
    recipientPhone = recipientPhone,
    messageSnapshot = messageSnapshot,
    openedBy = openedBy,
    openedAt = openedAt.toInstantOrNull(),
)

fun WhatsAppOpenLog.toDocument(): WhatsAppOpenLogDocument = WhatsAppOpenLogDocument(
    id = id,
    registrationId = registrationId,
    recipientPhone = recipientPhone,
    messageSnapshot = messageSnapshot,
    openedBy = openedBy,
    openedAt = openedAt.toTimestampOrNull(),
)

fun AuditLogDocument.toDomain(): AuditLog = AuditLog(
    id = id.requireValue("auditLogs.id"),
    actorId = actorId,
    action = action,
    entityType = entityType,
    entityId = entityId,
    before = before,
    after = after,
    createdAt = createdAt.toInstantOrNull(),
)

fun AuditLog.toDocument(): AuditLogDocument = AuditLogDocument(
    id = id,
    actorId = actorId,
    action = action,
    entityType = entityType,
    entityId = entityId,
    before = before,
    after = after,
    createdAt = createdAt.toTimestampOrNull(),
)

private fun Timestamp?.toInstantOrNull(): Instant? = this?.toDate()?.toInstant()

private fun Timestamp?.requireInstant(fieldName: String): Instant =
    toInstantOrNull() ?: throw FirestoreMappingException("Field $fieldName wajib tersedia")

private fun Instant.toTimestamp(): Timestamp = Timestamp(Date.from(this))

private fun Instant?.toTimestampOrNull(): Timestamp? = this?.toTimestamp()

private fun String.requireValue(fieldName: String): String =
    takeIf(String::isNotBlank)
        ?: throw FirestoreMappingException("Field $fieldName wajib tersedia")

private inline fun <reified T : Enum<T>> String.enumValue(fieldName: String): T =
    try {
        enumValueOf<T>(this)
    } catch (exception: IllegalArgumentException) {
        throw FirestoreMappingException(
            message = "Nilai '$this' pada field $fieldName tidak dikenal",
            cause = exception,
        )
    }
