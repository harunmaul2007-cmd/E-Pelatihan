package com.harunmaul.epelatihan.data.exporter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.harunmaul.epelatihan.R
import com.harunmaul.epelatihan.core.common.formatSchedule
import com.harunmaul.epelatihan.core.common.toRupiahString
import com.harunmaul.epelatihan.domain.exporter.ReportExporter
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.MonthlyReport
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AndroidReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReportExporter {

    private val xlsxGenerator = XlsxReportGenerator()

    override suspend fun exportXlsx(
        report: MonthlyReport,
        communityName: String,
        outputStream: OutputStream,
    ) = withContext(Dispatchers.IO) {
        xlsxGenerator.generate(report, communityName, outputStream)
    }

    override suspend fun exportPdf(
        report: MonthlyReport,
        communityName: String,
        outputStream: OutputStream,
    ) = withContext(Dispatchers.IO) {
        generatePdf(report, communityName, outputStream)
    }

    private fun generatePdf(
        report: MonthlyReport,
        communityName: String,
        outputStream: OutputStream,
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f

        val idLocale = Locale.forLanguageTag("id-ID")
        val monthStr = "${report.month.month.getDisplayName(TextStyle.FULL, idLocale)} ${report.month.year}"
        val printTimeStr = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", idLocale)
            .format(Instant.now().atZone(ZoneId.of("Asia/Jakarta"))) + " WIB"

        // Load logo
        val logoBitmap = try {
            val raw = BitmapFactory.decodeResource(context.resources, R.drawable.logo_epelatihan)
            Bitmap.createScaledBitmap(raw, 44, 44, true)
        } catch (_: Exception) {
            null
        }

        // Paints
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(33, 33, 33)
            textSize = 10f
        }
        val boldPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(33, 33, 33)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val grayPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(117, 117, 117)
            textSize = 8.5f
        }
        val linePaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(220, 220, 220)
            strokeWidth = 1f
        }
        val boxFillPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(246, 248, 250)
            style = Paint.Style.FILL
        }
        val tableHeaderFillPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(238, 242, 246)
            style = Paint.Style.FILL
        }
        val zebraFillPaint = Paint().apply {
            isAntiAlias = true
            color = Color.rgb(250, 250, 250)
            style = Paint.Style.FILL
        }

        var pageNumber = 1
        var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas

        fun drawHeader(c: Canvas, isFirstPage: Boolean): Float {
            var y = margin
            if (isFirstPage) {
                if (logoBitmap != null) {
                    c.drawBitmap(logoBitmap, margin, y, null)
                }
                val textStartX = if (logoBitmap != null) margin + 52f else margin

                boldPaint.textSize = 14f
                c.drawText(communityName, textStartX, y + 14f, boldPaint)

                boldPaint.textSize = 11f
                boldPaint.color = Color.rgb(21, 101, 192) // Primary blue
                c.drawText("LAPORAN BULANAN PELATIHAN", textStartX, y + 28f, boldPaint)
                boldPaint.color = Color.rgb(33, 33, 33)

                grayPaint.textSize = 9f
                c.drawText("Periode: $monthStr | Dicetak: $printTimeStr", textStartX, y + 42f, grayPaint)

                y += 56f
                c.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 16f
            } else {
                boldPaint.textSize = 10f
                c.drawText("$communityName - Laporan Pelatihan $monthStr", margin, y + 10f, boldPaint)
                grayPaint.textSize = 8.5f
                c.drawText("Dicetak: $printTimeStr", pageWidth - margin - 150f, y + 10f, grayPaint)
                y += 18f
                c.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 14f
            }
            return y
        }

        fun drawFooter(c: Canvas, pageNum: Int) {
            val footerY = pageHeight - margin + 8f
            c.drawLine(margin, footerY - 14f, pageWidth - margin, footerY - 14f, linePaint)
            grayPaint.textSize = 8f
            c.drawText("Dokumen Laporan Resmi E-Pelatihan", margin, footerY, grayPaint)
            val pageStr = "Halaman $pageNum"
            val width = grayPaint.measureText(pageStr)
            c.drawText(pageStr, pageWidth - margin - width, footerY, grayPaint)
        }

        var currentY = drawHeader(canvas, isFirstPage = true)

        // 1. Seksi Ringkasan Keuangan
        boldPaint.textSize = 11f
        canvas.drawText("Ringkasan Keuangan", margin, currentY, boldPaint)
        currentY += 8f

        // Box Keuangan
        val boxWidth = pageWidth - 2 * margin
        val boxHeight = 54f
        canvas.drawRect(margin, currentY, margin + boxWidth, currentY + boxHeight, boxFillPaint)
        canvas.drawRect(margin, currentY, margin + boxWidth, currentY + boxHeight, linePaint)

        val colWidth = boxWidth / 3f
        // Col 1: Total Tagihan
        grayPaint.textSize = 8.5f
        canvas.drawText("TOTAL TAGIHAN", margin + 12f, currentY + 18f, grayPaint)
        boldPaint.textSize = 12f
        canvas.drawText(report.totalBilled.toRupiahString(), margin + 12f, currentY + 38f, boldPaint)

        // Col 2: Total Pemasukan
        canvas.drawText("TOTAL PEMASUKAN", margin + colWidth + 12f, currentY + 18f, grayPaint)
        boldPaint.color = Color.rgb(46, 125, 50) // Green
        canvas.drawText(report.totalIncome.toRupiahString(), margin + colWidth + 12f, currentY + 38f, boldPaint)
        boldPaint.color = Color.rgb(33, 33, 33)

        // Col 3: Sisa Tagihan
        canvas.drawText("SISA TAGIHAN (PIUTANG)", margin + 2 * colWidth + 12f, currentY + 18f, grayPaint)
        boldPaint.color = Color.rgb(198, 40, 40) // Red
        canvas.drawText(report.outstanding.toRupiahString(), margin + 2 * colWidth + 12f, currentY + 38f, boldPaint)
        boldPaint.color = Color.rgb(33, 33, 33)

        currentY += boxHeight + 16f

        // 2. Seksi Ringkasan Peserta & Pelatihan
        boldPaint.textSize = 11f
        canvas.drawText("Ringkasan Peserta & Pelatihan", margin, currentY, boldPaint)
        currentY += 8f

        val boxPesertaHeight = 48f
        canvas.drawRect(margin, currentY, margin + boxWidth, currentY + boxPesertaHeight, boxFillPaint)
        canvas.drawRect(margin, currentY, margin + boxWidth, currentY + boxPesertaHeight, linePaint)

        val pColWidth = boxWidth / 4f
        // PCol 1: Pelatihan
        canvas.drawText("PELATIHAN", margin + 10f, currentY + 16f, grayPaint)
        boldPaint.textSize = 11f
        canvas.drawText("${report.trainingCount} Kegiatan", margin + 10f, currentY + 34f, boldPaint)

        // PCol 2: Pendaftar
        canvas.drawText("PENDAFTAR", margin + pColWidth + 10f, currentY + 16f, grayPaint)
        canvas.drawText("${report.registrationCount} Peserta", margin + pColWidth + 10f, currentY + 34f, boldPaint)

        // PCol 3: Status Pembayaran
        canvas.drawText("STATUS BAYAR", margin + 2 * pColWidth + 10f, currentY + 16f, grayPaint)
        canvas.drawText("${report.paidCount} Lunas / ${report.unpaidCount} Belum", margin + 2 * pColWidth + 10f, currentY + 34f, boldPaint)

        // PCol 4: Tarif
        canvas.drawText("SKEMA TARIF", margin + 3 * pColWidth + 10f, currentY + 16f, grayPaint)
        canvas.drawText("${report.earlyBirdCount} Early / ${report.normalCount} Normal", margin + 3 * pColWidth + 10f, currentY + 34f, boldPaint)

        currentY += boxPesertaHeight + 20f

        // 3. Rincian Pelatihan
        boldPaint.textSize = 11f
        canvas.drawText("Rincian per Pelatihan", margin, currentY, boldPaint)
        currentY += 8f

        fun drawTableHeader(c: Canvas, y: Float): Float {
            val thHeight = 22f
            c.drawRect(margin, y, pageWidth - margin, y + thHeight, tableHeaderFillPaint)
            c.drawRect(margin, y, pageWidth - margin, y + thHeight, linePaint)

            boldPaint.textSize = 8f
            c.drawText("NO", margin + 4f, y + 14f, boldPaint)
            c.drawText("PELATIHAN & METODE", margin + 24f, y + 14f, boldPaint)
            c.drawText("JADWAL", margin + 200f, y + 14f, boldPaint)
            c.drawText("PENDAFTAR (L/B)", margin + 310f, y + 14f, boldPaint)
            c.drawText("TOTAL TAGIHAN", margin + 395f, y + 14f, boldPaint)
            c.drawText("PEMASUKAN", margin + 470f, y + 14f, boldPaint)

            return y + thHeight
        }

        currentY = drawTableHeader(canvas, currentY)

        val rowHeight = 24f
        report.trainings.forEachIndexed { index, tr ->
            // Cek apakah muat di halaman saat ini
            if (currentY + rowHeight > pageHeight - margin - 20f) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)

                pageNumber++
                page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                currentY = drawHeader(canvas, isFirstPage = false)
                currentY = drawTableHeader(canvas, currentY)
            }

            if (index % 2 == 1) {
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, zebraFillPaint)
            }
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, linePaint)

            textPaint.textSize = 8f
            // No
            canvas.drawText("${index + 1}", margin + 6f, currentY + 15f, textPaint)

            // Nama & Metode (Truncate if needed)
            val methodBadge = if (tr.training.meetingMethod == MeetingMethod.ONLINE) "[Online]" else "[Offline]"
            val titleFull = "$methodBadge ${tr.training.name}"
            val titleTruncated = truncateText(titleFull, 168f, textPaint)
            canvas.drawText(titleTruncated, margin + 24f, currentY + 15f, textPaint)

            // Jadwal
            val schedStr = formatSchedule(tr.training.startAt, tr.training.endAt, tr.training.timeZoneId)
            val schedTruncated = truncateText(schedStr, 105f, textPaint)
            canvas.drawText(schedTruncated, margin + 200f, currentY + 15f, textPaint)

            // Pendaftar (L/B)
            val regStr = "${tr.registrationCount} (${tr.paidCount}/${tr.unpaidCount})"
            canvas.drawText(regStr, margin + 315f, currentY + 15f, textPaint)

            // Total Tagihan
            canvas.drawText(tr.totalBilled.toRupiahString(), margin + 395f, currentY + 15f, textPaint)

            // Total Pemasukan
            canvas.drawText(tr.totalIncome.toRupiahString(), margin + 470f, currentY + 15f, textPaint)

            currentY += rowHeight
        }

        // Baris Total di tabel jika ada pelatihan
        if (report.trainings.isNotEmpty()) {
            if (currentY + rowHeight > pageHeight - margin - 20f) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)

                pageNumber++
                page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                currentY = drawHeader(canvas, isFirstPage = false)
                currentY = drawTableHeader(canvas, currentY)
            }

            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, tableHeaderFillPaint)
            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, linePaint)

            boldPaint.textSize = 8.5f
            canvas.drawText("TOTAL", margin + 24f, currentY + 15f, boldPaint)
            canvas.drawText("${report.registrationCount} (${report.paidCount}/${report.unpaidCount})", margin + 315f, currentY + 15f, boldPaint)
            canvas.drawText(report.totalBilled.toRupiahString(), margin + 395f, currentY + 15f, boldPaint)
            canvas.drawText(report.totalIncome.toRupiahString(), margin + 470f, currentY + 15f, boldPaint)
        }

        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)

        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }

    private fun truncateText(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length - 1
        while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return text.substring(0, end) + ellipsis
    }
}
