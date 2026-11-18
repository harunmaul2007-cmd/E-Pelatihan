package com.harunmaul.epelatihan.data.exporter

import com.harunmaul.epelatihan.domain.model.MeetingMethod
import com.harunmaul.epelatihan.domain.model.MonthlyReport
import com.harunmaul.epelatihan.domain.model.PricingScheme
import com.harunmaul.epelatihan.domain.model.Training
import com.harunmaul.epelatihan.domain.model.TrainingLifecycle
import com.harunmaul.epelatihan.domain.model.TrainingReportRow
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.YearMonth
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxReportGeneratorTest {

    private val generator = XlsxReportGenerator()

    private val sampleTraining = Training(
        id = "t-1",
        name = "Pelatihan <Kotlin> & \"Android\" 'Jetpack'",
        description = "Deskripsi",
        startAt = Instant.parse("2026-05-10T10:00:00Z"),
        endAt = Instant.parse("2026-05-10T12:00:00Z"),
        timeZoneId = "Asia/Jakarta",
        speaker = "Pemateri",
        meetingMethod = MeetingMethod.ONLINE,
        zoomUrl = "https://zoom.us/j/123",
        location = null,
        recordingUrl = null,
        materialUrl = null,
        pricingScheme = PricingScheme.EARLY_BIRD_AND_NORMAL,
        normalPrice = 500_000,
        earlyBirdPrice = 300_000,
        lifecycle = TrainingLifecycle.ACTIVE,
        createdBy = "admin-1",
        createdAt = Instant.now(),
        updatedBy = "admin-1",
        updatedAt = Instant.now(),
    )

    private val sampleReport = MonthlyReport(
        month = YearMonth.of(2026, 5),
        trainingCount = 1,
        registrationCount = 3,
        paidCount = 2,
        unpaidCount = 1,
        earlyBirdCount = 1,
        normalCount = 2,
        totalBilled = 1_300_000,
        totalIncome = 800_000,
        outstanding = 500_000,
        trainings = listOf(
            TrainingReportRow(
                training = sampleTraining,
                registrationCount = 3,
                paidCount = 2,
                unpaidCount = 1,
                earlyBirdCount = 1,
                normalCount = 2,
                totalBilled = 1_300_000,
                totalIncome = 800_000,
                outstanding = 500_000,
            ),
        ),
        isFromCache = false,
    )

    @Test
    fun `generate produces valid zip containing all open xml parts`() {
        val out = ByteArrayOutputStream()
        generator.generate(sampleReport, "Komunitas Developer & Tech", out)

        val bytes = out.toByteArray()
        assertTrue("Output bytes should not be empty", bytes.isNotEmpty())

        val entries = mutableMapOf<String, String>()
        val zipIn = ZipInputStream(ByteArrayInputStream(bytes))
        var entry = zipIn.nextEntry
        while (entry != null) {
            val entryBytes = zipIn.readBytes()
            entries[entry.name] = String(entryBytes, Charsets.UTF_8)
            entry = zipIn.nextEntry
        }

        // Required Open XML parts
        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("_rels/.rels"))
        assertTrue(entries.containsKey("xl/_rels/workbook.xml.rels"))
        assertTrue(entries.containsKey("xl/workbook.xml"))
        assertTrue(entries.containsKey("xl/styles.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet1.xml"))
        assertTrue(entries.containsKey("xl/worksheets/sheet2.xml"))

        // Check Sheet 1 (Ringkasan)
        val sheet1 = entries["xl/worksheets/sheet1.xml"]
        assertNotNull(sheet1)
        assertTrue(sheet1!!.contains("Komunitas Developer &amp; Tech"))
        assertTrue(sheet1.contains("LAPORAN RINGKASAN BULANAN PELATIHAN"))
        assertTrue(sheet1.contains("Mei 2026"))
        // Numeric values as <v>
        assertTrue(sheet1.contains("<v>1300000</v>"))
        assertTrue(sheet1.contains("<v>800000</v>"))
        assertTrue(sheet1.contains("<v>500000</v>"))

        // Check Sheet 2 (Per Pelatihan)
        val sheet2 = entries["xl/worksheets/sheet2.xml"]
        assertNotNull(sheet2)
        // Verify XML escaping of special characters: <, >, &, ", '
        assertTrue(sheet2!!.contains("Pelatihan &lt;Kotlin&gt; &amp; &quot;Android&quot; &apos;Jetpack&apos;"))
        assertTrue(sheet2.contains("TOTAL"))
        assertTrue(sheet2.contains("<v>1300000</v>"))
    }

    @Test
    fun `escapeXml properly escapes xml reserved characters`() {
        val escaped = generator.escapeXml("A & B < C > D \" E ' F")
        assertEquals("A &amp; B &lt; C &gt; D &quot; E &apos; F", escaped)
    }
}
