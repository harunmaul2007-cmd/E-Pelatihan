package com.harunmaul.epelatihan.domain.exporter

import com.harunmaul.epelatihan.domain.model.MonthlyReport
import java.io.OutputStream

interface ReportExporter {
    suspend fun exportPdf(report: MonthlyReport, communityName: String, outputStream: OutputStream)
    suspend fun exportXlsx(report: MonthlyReport, communityName: String, outputStream: OutputStream)
}
