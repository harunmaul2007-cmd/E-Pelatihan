package com.harunmaul.epelatihan.data.firebase.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class AppSettingsDocument(
    var communityName: String = "",
    var schemaVersion: Int = 1,
    var hasAnyAdmin: Boolean = false,
    var updatedBy: String? = null,
    @get:ServerTimestamp
    var updatedAt: Timestamp? = null,
)

data class UserProfileDocument(
    @get:DocumentId
    var id: String = "",
    var displayName: String = "",
    var email: String = "",
    var role: String = "",
    var active: Boolean = false,
    var invitationStatus: String = "",
    var createdBy: String = "",
    @get:ServerTimestamp
    var createdAt: Timestamp? = null,
    var updatedBy: String = "",
    @get:ServerTimestamp
    var updatedAt: Timestamp? = null,
)

data class TrainingDocument(
    @get:DocumentId
    var id: String = "",
    var name: String = "",
    var description: String? = null,
    var startAt: Timestamp? = null,
    var endAt: Timestamp? = null,
    var timeZoneId: String = "",
    var speaker: String = "",
    var meetingMethod: String = "",
    var zoomUrl: String? = null,
    var location: String? = null,
    var recordingUrl: String? = null,
    var materialUrl: String? = null,
    var pricingScheme: String = "",
    var normalPrice: Long = 0,
    var earlyBirdPrice: Long? = null,
    var lifecycle: String = "",
    var createdBy: String = "",
    @get:ServerTimestamp
    var createdAt: Timestamp? = null,
    var updatedBy: String = "",
    @get:ServerTimestamp
    var updatedAt: Timestamp? = null,
)

data class ParticipantDocument(
    @get:DocumentId
    var id: String = "",
    var name: String = "",
    var nameSort: String = "",
    var phoneDisplay: String = "",
    var phoneNormalized: String = "",
    var email: String? = null,
    var notes: String? = null,
    var active: Boolean = true,
    var createdBy: String = "",
    @get:ServerTimestamp
    var createdAt: Timestamp? = null,
    var updatedBy: String = "",
    @get:ServerTimestamp
    var updatedAt: Timestamp? = null,
)

data class ParticipantPhoneIndexDocument(
    @get:DocumentId
    var phoneKey: String = "",
    var participantId: String = "",
    var phoneNormalized: String = "",
    @get:ServerTimestamp
    var createdAt: Timestamp? = null,
)

data class RegistrationDocument(
    @get:DocumentId
    var id: String = "",
    var trainingId: String = "",
    var participantId: String = "",
    var pricingType: String = "",
    var priceAmount: Long = 0,
    var paymentStatus: String = "",
    var paidAt: Timestamp? = null,
    var paidBy: String? = null,
    var createdBy: String = "",
    @get:ServerTimestamp
    var createdAt: Timestamp? = null,
    var updatedBy: String = "",
    @get:ServerTimestamp
    var updatedAt: Timestamp? = null,
)

data class WhatsAppOpenLogDocument(
    @get:DocumentId
    var id: String = "",
    var registrationId: String = "",
    var recipientPhone: String = "",
    var messageSnapshot: String = "",
    var openedBy: String = "",
    @get:ServerTimestamp
    var openedAt: Timestamp? = null,
)

data class AuditLogDocument(
    @get:DocumentId
    var id: String = "",
    var actorId: String = "",
    var action: String = "",
    var entityType: String = "",
    var entityId: String = "",
    var before: Map<String, Any?> = emptyMap(),
    var after: Map<String, Any?> = emptyMap(),
    @get:ServerTimestamp
    var createdAt: Timestamp? = null,
)
