package com.harunmaul.epelatihan.domain.usecase

import com.harunmaul.epelatihan.core.common.AppResult
import com.harunmaul.epelatihan.core.common.AuthorizationException
import com.harunmaul.epelatihan.core.common.InactiveAccountException
import com.harunmaul.epelatihan.domain.exporter.ReportExporter
import com.harunmaul.epelatihan.domain.model.MonthlyReport
import com.harunmaul.epelatihan.domain.model.UserSession
import com.harunmaul.epelatihan.domain.repository.AppSettingsRepository
import com.harunmaul.epelatihan.domain.repository.AuthRepository
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.flow.first

open class ExportReportPdfUseCase @Inject constructor(
    private val reportExporter: ReportExporter,
    private val appSettingsRepository: AppSettingsRepository,
    private val authRepository: AuthRepository,
) {
    open suspend operator fun invoke(report: MonthlyReport, outputStream: OutputStream): AppResult<Unit> {
        val session = authRepository.observeSession().first()
        val authenticated = session as? UserSession.Authenticated
            ?: return AppResult.Failure(AuthorizationException("Pengguna belum login."))

        if (!authenticated.profile.active) {
            return AppResult.Failure(InactiveAccountException())
        }

        return try {
            val appSettings = appSettingsRepository.getAppSettings()
            val communityName = appSettings.communityName.ifBlank { "E-Pelatihan" }
            reportExporter.exportPdf(report, communityName, outputStream)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure(e)
        }
    }
}
