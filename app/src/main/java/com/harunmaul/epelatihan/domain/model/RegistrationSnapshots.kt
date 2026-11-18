package com.harunmaul.epelatihan.domain.model

data class RegistrationListSnapshot(
    val registrations: List<Registration>,
    val isFromCache: Boolean = false,
)

data class RegistrationDetailSnapshot(
    val registration: Registration?,
    val isFromCache: Boolean = false,
)

data class RegistrationWithParticipant(
    val registration: Registration,
    val participant: Participant?,
)

data class TrainingRegistrationsSnapshot(
    val items: List<RegistrationWithParticipant>,
    val isFromCache: Boolean = false,
)
