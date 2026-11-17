package com.harunmaul.epelatihan.domain.model

import java.time.Instant

enum class UserRole { ADMIN, USER }
enum class InvitationStatus { INVITED, ACTIVE }
enum class MeetingMethod { ONLINE, OFFLINE }
enum class PricingScheme { NORMAL_ONLY, EARLY_BIRD_AND_NORMAL }
enum class PricingType { EARLY_BIRD, NORMAL }
enum class PaymentStatus { BELUM_BAYAR, LUNAS }
enum class TrainingLifecycle { ACTIVE, ARCHIVED }

data class AppSettings(
    val communityName: String,
    val logoUrl: String?,
    val logoStoragePath: String?,
    val schemaVersion: Int,
    val updatedBy: String?,
    val updatedAt: Instant?,
)

data class UserProfile(
    val id: String,
    val displayName: String,
    val email: String,
    val role: UserRole,
    val active: Boolean,
    val invitationStatus: InvitationStatus,
    val createdBy: String,
    val createdAt: Instant?,
    val updatedBy: String,
    val updatedAt: Instant?,
)

data class Training(
    val id: String,
    val name: String,
    val description: String?,
    val startAt: Instant,
    val endAt: Instant,
    val timeZoneId: String,
    val speaker: String,
    val meetingMethod: MeetingMethod,
    val zoomUrl: String?,
    val location: String?,
    val recordingUrl: String?,
    val materialUrl: String?,
    val pricingScheme: PricingScheme,
    val normalPrice: Long,
    val earlyBirdPrice: Long?,
    val lifecycle: TrainingLifecycle,
    val createdBy: String,
    val createdAt: Instant?,
    val updatedBy: String,
    val updatedAt: Instant?,
)

data class Participant(
    val id: String,
    val name: String,
    val nameSort: String,
    val phoneDisplay: String,
    val phoneNormalized: String,
    val email: String?,
    val notes: String?,
    val active: Boolean,
    val createdBy: String,
    val createdAt: Instant?,
    val updatedBy: String,
    val updatedAt: Instant?,
)

data class Registration(
    val id: String,
    val trainingId: String,
    val participantId: String,
    val pricingType: PricingType,
    val priceAmount: Long,
    val paymentStatus: PaymentStatus,
    val paidAt: Instant?,
    val paidBy: String?,
    val createdBy: String,
    val createdAt: Instant?,
    val updatedBy: String,
    val updatedAt: Instant?,
)

data class WhatsAppOpenLog(
    val id: String,
    val registrationId: String,
    val recipientPhone: String,
    val messageSnapshot: String,
    val openedBy: String,
    val openedAt: Instant?,
)

data class AuditLog(
    val id: String,
    val actorId: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val before: Map<String, Any?>,
    val after: Map<String, Any?>,
    val createdAt: Instant?,
)
