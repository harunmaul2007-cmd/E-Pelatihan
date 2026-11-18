package com.harunmaul.epelatihan.domain.model

import java.time.YearMonth

data class TrainingReportRow(
    val training: Training,
    val registrationCount: Int,
    val paidCount: Int,
    val unpaidCount: Int,
    val earlyBirdCount: Int,
    val normalCount: Int,
    val totalBilled: Long,
    val totalIncome: Long,
    val outstanding: Long,
)

data class MonthlyReport(
    val month: YearMonth,
    val trainingCount: Int,
    val registrationCount: Int,
    val paidCount: Int,
    val unpaidCount: Int,
    val earlyBirdCount: Int,
    val normalCount: Int,
    val totalBilled: Long,
    val totalIncome: Long,
    val outstanding: Long,
    val trainings: List<TrainingReportRow>,
    val isFromCache: Boolean = false,
)
