package com.harunmaul.epelatihan.domain.model

data class TrainingListSnapshot(
    val trainings: List<Training>,
    val isFromCache: Boolean = false,
)

data class TrainingDetailSnapshot(
    val training: Training?,
    val isFromCache: Boolean = false,
)
