package com.harunmaul.epelatihan.domain.validation

import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import java.time.Instant

enum class TrainingDisplayStatus {
    AKAN_DATANG,
    BERLANGSUNG,
    SELESAI,
    DIARSIPKAN,
}

object TrainingStatusResolver {
    fun resolve(
        training: Training,
        now: Instant = Instant.now(),
    ): TrainingDisplayStatus = when {
        training.lifecycle == TrainingLifecycle.ARCHIVED -> TrainingDisplayStatus.DIARSIPKAN
        now.isBefore(training.startAt) -> TrainingDisplayStatus.AKAN_DATANG
        now.isBefore(training.endAt) -> TrainingDisplayStatus.BERLANGSUNG
        else -> TrainingDisplayStatus.SELESAI
    }
}
