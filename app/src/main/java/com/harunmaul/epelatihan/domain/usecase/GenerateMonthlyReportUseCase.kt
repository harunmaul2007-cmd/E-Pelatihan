package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.domain.model.MonthlyReport
import com.harunmaul.epelatihan.domain.model.PaymentStatus
import com.harunmaul.epelatihan.domain.model.PricingType
import com.harunmaul.epelatihan.domain.model.TrainingReportRow
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import com.harunmaul.epelatihan.domain.repository.RegistrationRepository
import com.harunmaul.epelatihan.domain.repository.TrainingRepository
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.first

open class GenerateMonthlyReportUseCase @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val registrationRepository: RegistrationRepository,
    private val authRepository: AuthRepository,
) {
    open suspend operator fun invoke(targetMonth: YearMonth): AppResult<MonthlyReport> {
        val session = authRepository.observeSession().first()
        val authenticated = session as? UserSession.Authenticated
            ?: return AppResult.Failure(AuthorizationException("Pengguna belum login."))

        if (!authenticated.profile.active) {
            return AppResult.Failure(InactiveAccountException())
        }

        val trainingsSnapshotResult = trainingRepository.observeTrainings(includeArchived = true).first()
        val (trainings, isFromCache) = when (trainingsSnapshotResult) {
            is AppResult.Success -> trainingsSnapshotResult.value.trainings to trainingsSnapshotResult.value.isFromCache
            is AppResult.Failure -> return AppResult.Failure(trainingsSnapshotResult.cause)
        }

        val monthTrainings = trainings.filter { training ->
            val zoneId = try {
                ZoneId.of(training.timeZoneId)
            } catch (_: Exception) {
                ZoneId.of("Asia/Jakarta")
            }
            val trainingMonth = YearMonth.from(training.startAt.atZone(zoneId))
            trainingMonth == targetMonth
        }.sortedBy { it.startAt }

        val trainingRows = mutableListOf<TrainingReportRow>()
        var totalBilledAll = 0L
        var totalIncomeAll = 0L
        var totalRegistrationsAll = 0
        var totalPaidAll = 0
        var totalUnpaidAll = 0
        var totalEarlyBirdAll = 0
        var totalNormalAll = 0

        for (training in monthTrainings) {
            val regResult = registrationRepository.getRegistrationsByTraining(training.id)
            val registrations = when (regResult) {
                is AppResult.Success -> regResult.value
                is AppResult.Failure -> emptyList()
            }

            val regCount = registrations.size
            val paidCount = registrations.count { it.paymentStatus == PaymentStatus.LUNAS }
            val unpaidCount = registrations.count { it.paymentStatus == PaymentStatus.BELUM_BAYAR }
            val earlyBirdCount = registrations.count { it.pricingType == PricingType.EARLY_BIRD }
            val normalCount = registrations.count { it.pricingType == PricingType.NORMAL }

            val totalBilled = registrations.sumOf { it.priceAmount }
            val totalIncome = registrations.filter { it.paymentStatus == PaymentStatus.LUNAS }.sumOf { it.priceAmount }
            val outstanding = totalBilled - totalIncome

            trainingRows.add(
                TrainingReportRow(
                    training = training,
                    registrationCount = regCount,
                    paidCount = paidCount,
                    unpaidCount = unpaidCount,
                    earlyBirdCount = earlyBirdCount,
                    normalCount = normalCount,
                    totalBilled = totalBilled,
                    totalIncome = totalIncome,
                    outstanding = outstanding,
                ),
            )

            totalBilledAll += totalBilled
            totalIncomeAll += totalIncome
            totalRegistrationsAll += regCount
            totalPaidAll += paidCount
            totalUnpaidAll += unpaidCount
            totalEarlyBirdAll += earlyBirdCount
            totalNormalAll += normalCount
        }

        val report = MonthlyReport(
            month = targetMonth,
            trainingCount = monthTrainings.size,
            registrationCount = totalRegistrationsAll,
            paidCount = totalPaidAll,
            unpaidCount = totalUnpaidAll,
            earlyBirdCount = totalEarlyBirdAll,
            normalCount = totalNormalAll,
            totalBilled = totalBilledAll,
            totalIncome = totalIncomeAll,
            outstanding = totalBilledAll - totalIncomeAll,
            trainings = trainingRows,
            isFromCache = isFromCache,
        )

        return AppResult.Success(report)
    }
}
