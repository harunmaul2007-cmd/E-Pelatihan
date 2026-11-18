package com.harunmaul.epelatihan.domain.model

data class ParticipantListSnapshot(
    val participants: List<Participant>,
    val isFromCache: Boolean = false,
)

data class ParticipantDetailSnapshot(
    val participant: Participant?,
    val isFromCache: Boolean = false,
)
