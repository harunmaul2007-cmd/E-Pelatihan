package com.harunmaul.epelatihan.data.exporter

import com.harunmaul.epelatihan.core.common.formatSchedule
import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.MonthlyReport
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.format.TextStyle
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxReportGenerator {

    fun generate(report: MonthlyReport, communityName: String, outputStream: OutputStream) {
        val zip = ZipOutputStream(outputStream)

        writeEntry(zip, "[Content_Types].xml", buildContentTypesXml())
        writeEntry(zip, "_rels/.rels", buildRootRelsXml())
        writeEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())
        writeEntry(zip, "xl/workbook.xml", buildWorkbookXml())
        writeEntry(zip, "xl/styles.xml", buildStylesXml())
        writeEntry(zip, "xl/worksheets/sheet1.xml", buildSheet1RingkasanXml(report, communityName))
        writeEntry(zip, "xl/worksheets/sheet2.xml", buildSheet2PelatihanXml(report, communityName))

        zip.finish()
        zip.flush()
    }

    private fun writeEntry(zip: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zip.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zip.write(bytes, 0, bytes.size)
        zip.closeEntry()
    }

    private fun buildContentTypesXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".trimIndent()

    private fun buildRootRelsXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()

    private fun buildWorkbookRelsXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent()

    private fun buildWorkbookXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Ringkasan" sheetId="1" r:id="rId1"/>
    <sheet name="Per Pelatihan" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>""".trimIndent()

    private fun buildStylesXml(): String =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="1">
    <numFmt numFmtId="164" formatCode="&quot;Rp&quot;\ #,##0"/>
  </numFmts>
  <fonts count="3">
    <font><sz val="11"/><color theme="1"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color theme="1"/><name val="Calibri"/></font>
    <font><b/><sz val="14"/><color theme="1"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFEAEAEA"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFB0B0B0"/></left>
      <right style="thin"><color rgb="FFB0B0B0"/></right>
      <top style="thin"><color rgb="FFB0B0B0"/></top>
      <bottom style="thin"><color rgb="FFB0B0B0"/></bottom>
      <diagonal/>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="9">
    <!-- 0: Normal default text -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <!-- 1: Title bold -->
    <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <!-- 2: Table header (Bold, background, center, border) -->
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 3: Data text with border -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <!-- 4: Currency with border (right-aligned) -->
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right"/>
    </xf>
    <!-- 5: Integer with border (center-aligned) -->
    <xf numFmtId="1" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center"/>
    </xf>
    <!-- 6: Summary bold label with border & fill -->
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <!-- 7: Summary bold currency with border & fill -->
    <xf numFmtId="164" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="right"/>
    </xf>
    <!-- 8: Summary bold integer with border & fill -->
    <xf numFmtId="1" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyNumberFormat="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center"/>
    </xf>
  </cellXfs>
</styleSheet>""".trimIndent()

    private fun buildSheet1RingkasanXml(report: MonthlyReport, communityName: String): String {
        val idLocale = Locale.forLanguageTag("id-ID")
        val monthStr = "${report.month.month.getDisplayName(TextStyle.FULL, idLocale)} ${report.month.year}"

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<cols>""")
        sb.append("""<col min="1" max="1" width="32" customWidth="1"/>""")
        sb.append("""<col min="2" max="2" width="24" customWidth="1"/>""")
        sb.append("""</cols>""")
        sb.append("""<sheetData>""")

        var row = 1
        // Row 1: Komunitas
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, escapeXml(communityName), style = 1))
        sb.append("""</row>""")
        row++

        // Row 2: Judul
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "LAPORAN RINGKASAN BULANAN PELATIHAN", style = 1))
        sb.append("""</row>""")
        row++

        // Row 3: Periode
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Periode: $monthStr", style = 0))
        sb.append("""</row>""")
        row += 2

        // Row 5: Header Keuangan
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Ringkasan Keuangan", style = 2))
        sb.append(cellStr("B", row, "Nilai (Rupiah)", style = 2))
        sb.append("""</row>""")
        row++

        // Row 6: Total Tagihan
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Total Tagihan", style = 3))
        sb.append(cellNum("B", row, report.totalBilled, style = 4))
        sb.append("""</row>""")
        row++

        // Row 7: Total Pemasukan
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Total Pemasukan (Lunas)", style = 3))
        sb.append(cellNum("B", row, report.totalIncome, style = 4))
        sb.append("""</row>""")
        row++

        // Row 8: Sisa Belum Dibayar
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Sisa Belum Dibayar (Piutang)", style = 6))
        sb.append(cellNum("B", row, report.outstanding, style = 7))
        sb.append("""</row>""")
        row += 2

        // Row 10: Header Peserta & Pelatihan
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Ringkasan Peserta & Pelatihan", style = 2))
        sb.append(cellStr("B", row, "Jumlah", style = 2))
        sb.append("""</row>""")
        row++

        // Row 11: Jumlah Pelatihan
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Jumlah Kegiatan Pelatihan", style = 3))
        sb.append(cellNum("B", row, report.trainingCount.toLong(), style = 5))
        sb.append("""</row>""")
        row++

        // Row 12: Total Seluruh Pendaftar
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Total Seluruh Pendaftar", style = 3))
        sb.append(cellNum("B", row, report.registrationCount.toLong(), style = 5))
        sb.append("""</row>""")
        row++

        // Row 13: Peserta Lunas
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Peserta Lunas", style = 3))
        sb.append(cellNum("B", row, report.paidCount.toLong(), style = 5))
        sb.append("""</row>""")
        row++

        // Row 14: Peserta Belum Bayar
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Peserta Belum Bayar", style = 3))
        sb.append(cellNum("B", row, report.unpaidCount.toLong(), style = 5))
        sb.append("""</row>""")
        row++

        // Row 15: Pendaftar Early Bird
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Pendaftar Tarif Early Bird", style = 3))
        sb.append(cellNum("B", row, report.earlyBirdCount.toLong(), style = 5))
        sb.append("""</row>""")
        row++

        // Row 16: Pendaftar Normal
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "Pendaftar Tarif Normal", style = 3))
        sb.append(cellNum("B", row, report.normalCount.toLong(), style = 5))
        sb.append("""</row>""")

        sb.append("""</sheetData>""")
        sb.append("""</worksheet>""")
        return sb.toString()
    }

    private fun buildSheet2PelatihanXml(report: MonthlyReport, communityName: String): String {
        val idLocale = Locale.forLanguageTag("id-ID")
        val monthStr = "${report.month.month.getDisplayName(TextStyle.FULL, idLocale)} ${report.month.year}"

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<cols>""")
        sb.append("""<col min="1" max="1" width="6" customWidth="1"/>""")   // No
        sb.append("""<col min="2" max="2" width="34" customWidth="1"/>""")  // Pelatihan
        sb.append("""<col min="3" max="3" width="12" customWidth="1"/>""")  // Metode
        sb.append("""<col min="4" max="4" width="30" customWidth="1"/>""")  // Jadwal
        sb.append("""<col min="5" max="5" width="12" customWidth="1"/>""")  // Pendaftar
        sb.append("""<col min="6" max="6" width="10" customWidth="1"/>""")  // Lunas
        sb.append("""<col min="7" max="7" width="12" customWidth="1"/>""")  // Belum
        sb.append("""<col min="8" max="8" width="12" customWidth="1"/>""")  // Early
        sb.append("""<col min="9" max="9" width="12" customWidth="1"/>""")  // Normal
        sb.append("""<col min="10" max="10" width="18" customWidth="1"/>""") // Tagihan
        sb.append("""<col min="11" max="11" width="18" customWidth="1"/>""") // Pemasukan
        sb.append("""<col min="12" max="12" width="18" customWidth="1"/>""") // Sisa
        sb.append("""</cols>""")
        sb.append("""<sheetData>""")

        var row = 1
        // Title
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, escapeXml(communityName), style = 1))
        sb.append("""</row>""")
        row++

        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "RINCIAN LAPORAN PER PELATIHAN - $monthStr", style = 1))
        sb.append("""</row>""")
        row += 2

        // Table Header
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "No", style = 2))
        sb.append(cellStr("B", row, "Nama Pelatihan", style = 2))
        sb.append(cellStr("C", row, "Metode", style = 2))
        sb.append(cellStr("D", row, "Jadwal", style = 2))
        sb.append(cellStr("E", row, "Pendaftar", style = 2))
        sb.append(cellStr("F", row, "Lunas", style = 2))
        sb.append(cellStr("G", row, "Belum Bayar", style = 2))
        sb.append(cellStr("H", row, "Early Bird", style = 2))
        sb.append(cellStr("I", row, "Normal", style = 2))
        sb.append(cellStr("J", row, "Total Tagihan", style = 2))
        sb.append(cellStr("K", row, "Total Pemasukan", style = 2))
        sb.append(cellStr("L", row, "Sisa Tagihan", style = 2))
        sb.append("""</row>""")
        row++

        // Table Rows
        report.trainings.forEachIndexed { index, tr ->
            val methodLabel = if (tr.training.meetingMethod == MeetingMethod.ONLINE) "Online" else "Offline"
            val schedule = formatSchedule(tr.training.startAt, tr.training.endAt, tr.training.timeZoneId)

            sb.append("""<row r="$row">""")
            sb.append(cellNum("A", row, (index + 1).toLong(), style = 5))
            sb.append(cellStr("B", row, escapeXml(tr.training.name), style = 3))
            sb.append(cellStr("C", row, methodLabel, style = 3))
            sb.append(cellStr("D", row, escapeXml(schedule), style = 3))
            sb.append(cellNum("E", row, tr.registrationCount.toLong(), style = 5))
            sb.append(cellNum("F", row, tr.paidCount.toLong(), style = 5))
            sb.append(cellNum("G", row, tr.unpaidCount.toLong(), style = 5))
            sb.append(cellNum("H", row, tr.earlyBirdCount.toLong(), style = 5))
            sb.append(cellNum("I", row, tr.normalCount.toLong(), style = 5))
            sb.append(cellNum("J", row, tr.totalBilled, style = 4))
            sb.append(cellNum("K", row, tr.totalIncome, style = 4))
            sb.append(cellNum("L", row, tr.outstanding, style = 4))
            sb.append("""</row>""")
            row++
        }

        // Summary Total Row
        sb.append("""<row r="$row">""")
        sb.append(cellStr("A", row, "", style = 6))
        sb.append(cellStr("B", row, "TOTAL", style = 6))
        sb.append(cellStr("C", row, "", style = 6))
        sb.append(cellStr("D", row, "", style = 6))
        sb.append(cellNum("E", row, report.registrationCount.toLong(), style = 8))
        sb.append(cellNum("F", row, report.paidCount.toLong(), style = 8))
        sb.append(cellNum("G", row, report.unpaidCount.toLong(), style = 8))
        sb.append(cellNum("H", row, report.earlyBirdCount.toLong(), style = 8))
        sb.append(cellNum("I", row, report.normalCount.toLong(), style = 8))
        sb.append(cellNum("J", row, report.totalBilled, style = 7))
        sb.append(cellNum("K", row, report.totalIncome, style = 7))
        sb.append(cellNum("L", row, report.outstanding, style = 7))
        sb.append("""</row>""")

        sb.append("""</sheetData>""")
        sb.append("""</worksheet>""")
        return sb.toString()
    }

    private fun cellStr(col: String, row: Int, value: String, style: Int): String {
        return """<c r="$col$row" t="inlineStr" s="$style"><is><t>$value</t></is></c>"""
    }

    private fun cellNum(col: String, row: Int, value: Long, style: Int): String {
        return """<c r="$col$row" s="$style"><v>$value</v></c>"""
    }

    fun escapeXml(text: String): String {
        return buildString {
            for (char in text) {
                when (char) {
                    '&' -> append("&amp;")
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '"' -> append("&quot;")
                    '\'' -> append("&apos;")
                    else -> append(char)
                }
            }
        }
    }
}
